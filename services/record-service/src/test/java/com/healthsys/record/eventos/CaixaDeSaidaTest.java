package com.healthsys.record.eventos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthsys.record.prontuario.Profissional;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Teste de tolerância a falhas da mensageria: com o Kafka fora do ar nenhum evento se perde. */
class CaixaDeSaidaTest {

    private final EventoPendenteRepository repository = mock(EventoPendenteRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final PublicadorDeEventos publicador =
            new PublicadorDeEventos(repository, kafka, new SimpleMeterRegistry(), 100);

    private static EventoPendente evento(String id) {
        return new EventoPendente(id, RegistroDeEventos.TOPICO, "1", "{\"eventoId\":\"" + id + "\"}", Instant.now());
    }

    @Test
    void eventoGravadoTemOFormatoCombinadoEntreOsServicos() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Clock relogio = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC);
        RegistroDeEventos registro = new RegistroDeEventos(repository, mapper, relogio);

        registro.registrar("EXAME_SOLICITADO", 1, "Maria Silva", new Profissional(7, "Dra. Ana", "MEDICO"),
                Map.of("exameId", "e1"));

        ArgumentCaptor<EventoPendente> gravado = ArgumentCaptor.forClass(EventoPendente.class);
        verify(repository).save(gravado.capture());
        assertThat(gravado.getValue().getTopico()).isEqualTo("prontuario.eventos");
        assertThat(gravado.getValue().getChave()).isEqualTo("1");
        assertThat(gravado.getValue().getEnviadoEm()).isNull();

        JsonNode json = mapper.readTree(gravado.getValue().getPayload());
        assertThat(json.get("eventoId").asText()).isEqualTo(gravado.getValue().getEventoId());
        assertThat(json.get("tipo").asText()).isEqualTo("EXAME_SOLICITADO");
        assertThat(json.get("origem").asText()).isEqualTo("record-service");
        assertThat(json.get("ocorridoEm").asText()).isEqualTo("2026-11-22T10:00:00Z");
        assertThat(json.get("pacienteNome").asText()).isEqualTo("Maria Silva");
        assertThat(json.get("autor").get("perfil").asText()).isEqualTo("MEDICO");
        assertThat(json.get("dados").get("exameId").asText()).isEqualTo("e1");
    }

    @Test
    void comOKafkaNoArOEventoEEnviadoEMarcado() {
        EventoPendente e = evento("a");
        when(repository.findTop50ByEnviadoEmIsNullOrderByCriadoEmAsc()).thenReturn(List.of(e));
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        publicador.publicarPendentes();

        assertThat(e.getEnviadoEm()).isNotNull();
        verify(repository).save(e);
    }

    @Test
    void comOKafkaForaDoArOEventoContinuaGuardadoEParaNaOrdem() {
        EventoPendente primeiro = evento("a");
        EventoPendente segundo = evento("b");
        when(repository.findTop50ByEnviadoEmIsNullOrderByCriadoEmAsc()).thenReturn(List.of(primeiro, segundo));
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka fora do ar")));

        publicador.publicarPendentes();

        assertThat(primeiro.getEnviadoEm()).isNull();
        assertThat(primeiro.getTentativas()).isEqualTo(1);
        assertThat(primeiro.getUltimoErro()).contains("Kafka fora do ar");
        // Não pula para o segundo: a ordem dos acontecimentos é mantida.
        verify(kafka, times(1)).send(anyString(), anyString(), anyString());
        assertThat(segundo.getTentativas()).isZero();
    }

    @Test
    void quandoOKafkaVoltaOsEventosGuardadosSaoEnviados() {
        EventoPendente e = evento("a");
        when(repository.findTop50ByEnviadoEmIsNullOrderByCriadoEmAsc()).thenReturn(List.of(e));
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("fora do ar")))
                .thenReturn(CompletableFuture.completedFuture(null));

        publicador.publicarPendentes();
        assertThat(e.getEnviadoEm()).isNull();

        publicador.publicarPendentes();
        assertThat(e.getEnviadoEm()).isNotNull();
        assertThat(e.getUltimoErro()).isNull();
    }

    @Test
    void semEventosPendentesNaoChamaOKafka() {
        when(repository.findTop50ByEnviadoEmIsNullOrderByCriadoEmAsc()).thenReturn(List.of());

        publicador.publicarPendentes();

        verify(kafka, never()).send(anyString(), anyString(), anyString());
        verify(repository, never()).save(any());
    }
}
