package com.healthsys.notification.notificacao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthsys.notification.tempo_real.CanalTempoReal;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Recebe os eventos, guarda as notificações e entrega em tempo real.
 * Idempotente: o mesmo evento recebido duas vezes (o Kafka entrega "pelo menos uma vez") vira uma notificação só.
 */
@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private static final int LIMITE_DA_LISTA = 50;

    private final NotificacaoRepository repository;
    private final MongoTemplate mongo;
    private final CanalTempoReal tempoReal;
    private final ObjectMapper mapper;
    private final MeterRegistry metricas;
    private final Clock relogio;

    @Autowired
    public NotificacaoService(NotificacaoRepository repository, MongoTemplate mongo, CanalTempoReal tempoReal,
                              ObjectMapper mapper, MeterRegistry metricas) {
        this(repository, mongo, tempoReal, mapper, metricas, Clock.systemUTC());
    }

    NotificacaoService(NotificacaoRepository repository, MongoTemplate mongo, CanalTempoReal tempoReal,
                       ObjectMapper mapper, MeterRegistry metricas, Clock relogio) {
        this.repository = repository;
        this.mongo = mongo;
        this.tempoReal = tempoReal;
        this.mapper = mapper;
        this.metricas = metricas;
        this.relogio = relogio;
    }

    /**
     * Processa uma mensagem do Kafka. JSON inválido lança erro: o tratamento do Kafka tenta de novo
     * e, se continuar inválido, manda para o tópico de mensagens com problema (DLT).
     */
    public Optional<Notificacao> receber(String mensagem) {
        JsonNode evento;
        try {
            evento = mapper.readTree(mensagem);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Evento com JSON inválido", e);
        }
        if (evento == null || !evento.isObject()) {
            throw new IllegalArgumentException("Evento vazio ou fora do formato combinado");
        }
        Optional<Notificacao> traduzida = RegrasDeNotificacao.traduzir(evento, Instant.now(relogio));
        if (traduzida.isEmpty()) {
            return Optional.empty();
        }
        Notificacao notificacao = traduzida.get();
        if (repository.existsById(notificacao.getId())) {
            log.info("Evento {} já processado antes: ignorado (entrega repetida)", notificacao.getId());
            return Optional.empty();
        }
        Notificacao salva = repository.save(notificacao);
        Counter.builder("healthsys.notificacoes")
                .description("Notificações geradas a partir dos eventos do Kafka")
                .tag("tipo", salva.getTipo())
                .tag("gravidade", salva.getGravidade().name())
                .register(metricas)
                .increment();
        tempoReal.publicar(salva);
        return Optional.of(salva);
    }

    /** As 50 mais recentes da pessoa logada. */
    public List<NotificacaoResponse> listar(Profissional quem) {
        Query consulta = Query.query(paraQuem(quem))
                .with(Sort.by(Sort.Direction.DESC, "criadaEm"))
                .limit(LIMITE_DA_LISTA);
        return mongo.find(consulta, Notificacao.class).stream()
                .map(n -> NotificacaoResponse.de(n, quem.id()))
                .toList();
    }

    public long contarNaoLidas(Profissional quem) {
        return mongo.count(Query.query(naoLidasPor(quem)), Notificacao.class);
    }

    /** Marca uma como lida. Devolve false se ela não existe ou não é para essa pessoa. */
    public boolean marcarLida(String id, Profissional quem) {
        if (quem.id() == null) {
            return false;
        }
        Query consulta = Query.query(new Criteria().andOperator(Criteria.where("_id").is(id), paraQuem(quem)));
        return mongo.updateFirst(consulta, new Update().addToSet("lidaPor", quem.id()), Notificacao.class)
                .getMatchedCount() > 0;
    }

    public long marcarTodasLidas(Profissional quem) {
        if (quem.id() == null) {
            return 0;
        }
        return mongo.updateMulti(Query.query(naoLidasPor(quem)), new Update().addToSet("lidaPor", quem.id()),
                Notificacao.class).getModifiedCount();
    }

    /** Enviadas diretamente para a pessoa, ou para o perfil dela (sem destinatário específico). */
    static Criteria paraQuem(Profissional quem) {
        List<Criteria> alvos = new ArrayList<>();
        if (quem.id() != null) {
            alvos.add(Criteria.where("usuarioDestinoId").is(quem.id()));
        }
        alvos.add(new Criteria().andOperator(
                Criteria.where("usuarioDestinoId").is(null),
                Criteria.where("perfisDestino").is(quem.perfil())));
        return new Criteria().orOperator(alvos.toArray(new Criteria[0]));
    }

    private static Criteria naoLidasPor(Profissional quem) {
        if (quem.id() == null) {
            return paraQuem(quem);
        }
        return new Criteria().andOperator(paraQuem(quem), Criteria.where("lidaPor").ne(quem.id()));
    }
}
