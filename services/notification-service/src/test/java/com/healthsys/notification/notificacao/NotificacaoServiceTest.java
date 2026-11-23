package com.healthsys.notification.notificacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthsys.notification.tempo_real.CanalTempoReal;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoServiceTest {

    private static final String EVENTO = """
            {"eventoId":"ev-9","tipo":"TRIAGEM_ABERTA","origem":"triage-service","pacienteId":1,
             "pacienteNome":"Maria","autor":{"nome":"Carla","perfil":"RECEPCIONISTA"},"dados":{"queixaPrincipal":"Febre"}}
            """;

    private final NotificacaoRepository repository = mock(NotificacaoRepository.class);
    private final CanalTempoReal tempoReal = mock(CanalTempoReal.class);
    private final SimpleMeterRegistry metricas = new SimpleMeterRegistry();
    private final NotificacaoService service = new NotificacaoService(repository, mock(MongoTemplate.class), tempoReal,
            new ObjectMapper(), metricas, Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void eventoNovoViraNotificacaoEEEntregueNaHora() {
        when(repository.existsById("ev-9")).thenReturn(false);
        when(repository.save(any(Notificacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Notificacao> n = service.receber(EVENTO);

        assertThat(n).isPresent();
        verify(tempoReal).publicar(n.get());
        assertThat(metricas.get("healthsys.notificacoes").counter().count()).isEqualTo(1.0);
    }

    @Test
    void mesmoEventoRecebidoDuasVezesNaoDuplicaANotificacao() {
        when(repository.existsById("ev-9")).thenReturn(true);

        assertThat(service.receber(EVENTO)).isEmpty();
        verify(repository, never()).save(any());
        verify(tempoReal, never()).publicar(any());
    }

    @Test
    void mensagemInvalidaGeraErroParaIrAoDlt() {
        assertThatThrownBy(() -> service.receber("isto não é json")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.receber("[]")).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void eventoSemAvisoNaoGravaNada() {
        assertThat(service.receber("{\"eventoId\":\"ev-3\",\"tipo\":\"CONSULTA_REGISTRADA\"}")).isEmpty();
        verify(repository, never()).save(any());
    }
}
