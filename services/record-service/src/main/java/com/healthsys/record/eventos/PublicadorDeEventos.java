package com.healthsys.record.eventos;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Envia para o Kafka os eventos que estão na caixa de saída, a cada poucos segundos.
 * - Envia um de cada vez, na ordem; só marca como enviado depois que o Kafka confirma.
 * - Se o Kafka não responder, para e tenta de novo na próxima rodada (o evento continua guardado).
 * - Entrega "pelo menos uma vez": quem consome ignora repetidos pelo eventoId.
 * A métrica healthsys_eventos_pendentes (Prometheus/Grafana) mostra quantos estão esperando.
 */
@Component
public class PublicadorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorDeEventos.class);

    private final EventoPendenteRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final long timeoutEnvioMs;

    public PublicadorDeEventos(EventoPendenteRepository repository, KafkaTemplate<String, String> kafka,
                               MeterRegistry metricas,
                               @Value("${eventos.timeout-envio-ms:5000}") long timeoutEnvioMs) {
        this.repository = repository;
        this.kafka = kafka;
        this.timeoutEnvioMs = timeoutEnvioMs;
        Gauge.builder("healthsys.eventos.pendentes", () -> repository.countByEnviadoEmIsNull())
                .description("Eventos gravados que ainda não foram entregues ao Kafka")
                .register(metricas);
    }

    @Scheduled(fixedDelayString = "${eventos.intervalo-ms:2000}", initialDelayString = "${eventos.atraso-inicial-ms:5000}")
    public void publicarPendentes() {
        for (EventoPendente evento : repository.findTop50ByEnviadoEmIsNullOrderByCriadoEmAsc()) {
            try {
                kafka.send(evento.getTopico(), evento.getChave(), evento.getPayload())
                        .get(timeoutEnvioMs, TimeUnit.MILLISECONDS);
                evento.marcarEnviado(Instant.now());
                repository.save(evento);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                evento.registrarFalha(e.getClass().getSimpleName() + ": " + e.getMessage());
                repository.save(evento);
                log.warn("Kafka indisponível: o evento {} continua guardado e será enviado depois ({})",
                        evento.getEventoId(), e.getMessage());
                return;
            }
        }
    }

    /** Uma vez por dia apaga os eventos já entregues há mais de 7 dias. */
    @Scheduled(cron = "${eventos.limpeza-cron:0 0 3 * * *}")
    public void limparEnviados() {
        long apagados = repository.deleteByEnviadoEmBefore(Instant.now().minus(Duration.ofDays(7)));
        if (apagados > 0) {
            log.info("{} eventos antigos já enviados foram apagados da caixa de saída", apagados);
        }
    }
}
