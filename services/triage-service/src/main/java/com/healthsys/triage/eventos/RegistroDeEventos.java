package com.healthsys.triage.eventos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthsys.triage.triagem.Profissional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Monta o evento (JSON) e grava na caixa de saída, dentro da transação de quem chamou.
 * Formato igual em todos os serviços:
 * { eventoId, tipo, origem, ocorridoEm, pacienteId, pacienteNome, autor { id, nome, perfil }, dados { ... } }
 */
@Component
public class RegistroDeEventos {

    public static final String TOPICO = "triagem.eventos";
    static final String ORIGEM = "triage-service";

    private final EventoPendenteRepository repository;
    private final ObjectMapper mapper;
    private final Clock relogio;

    @Autowired
    public RegistroDeEventos(EventoPendenteRepository repository, ObjectMapper mapper) {
        this(repository, mapper, Clock.systemUTC());
    }

    RegistroDeEventos(EventoPendenteRepository repository, ObjectMapper mapper, Clock relogio) {
        this.repository = repository;
        this.mapper = mapper;
        this.relogio = relogio;
    }

    /** autor pode ser nulo quando o evento é gerado pelo próprio sistema (ex.: alerta de um monitor). */
    public void registrar(String tipo, Integer pacienteId, String pacienteNome, Profissional autor, Map<String, Object> dados) {
        String eventoId = UUID.randomUUID().toString();
        Instant agora = Instant.now(relogio);

        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("eventoId", eventoId);
        evento.put("tipo", tipo);
        evento.put("origem", ORIGEM);
        evento.put("ocorridoEm", agora.toString());
        evento.put("pacienteId", pacienteId);
        evento.put("pacienteNome", pacienteNome);
        if (autor != null) {
            Map<String, Object> quem = new LinkedHashMap<>();
            quem.put("id", autor.id());
            quem.put("nome", autor.nome());
            quem.put("perfil", autor.perfil());
            evento.put("autor", quem);
        }
        evento.put("dados", dados == null ? Map.of() : dados);

        String json;
        try {
            json = mapper.writeValueAsString(evento);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível montar o evento " + tipo, e);
        }
        String chave = pacienteId == null ? null : String.valueOf(pacienteId);
        repository.save(new EventoPendente(eventoId, TOPICO, chave, json, agora));
    }
}
