package com.healthsys.monitoring.sinais;

import com.healthsys.monitoring.eventos.RegistroDeEventos;
import com.healthsys.monitoring.leito.Leito;
import com.healthsys.monitoring.leito.LeitoRepository;
import com.healthsys.monitoring.leito.Profissional;
import com.healthsys.monitoring.leito.Setor;
import com.healthsys.monitoring.sinais.dto.MonitorLeitoResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoramentoServiceTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC);
    private static final Profissional ENFERMEIRO = new Profissional(8, "Enf. Bruno", "ENFERMEIRO");

    private LeitoRepository leitos;
    private AlertaRepository alertas;
    private PainelDeLeituras painel;
    private RegistroDeEventos eventos;
    private MonitoramentoService service;

    @BeforeEach
    void setUp() {
        leitos = mock(LeitoRepository.class);
        alertas = mock(AlertaRepository.class);
        painel = new PainelDeLeituras();
        eventos = mock(RegistroDeEventos.class);
        service = new MonitoramentoService(leitos, alertas, painel, eventos, new SimpleMeterRegistry(), RELOGIO);
        when(alertas.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(alertas.findFirstByLeitoCodigoAndTipoOrderByCriadoEmDesc(any(), any())).thenReturn(Optional.empty());
    }

    private static Leito leitoOcupado() {
        Leito l = new Leito("UTI-01", Setor.UTI, ENFERMEIRO, LocalDateTime.of(2026, 11, 22, 8, 0));
        l.ocupar(1, "Maria Silva", ENFERMEIRO, LocalDateTime.of(2026, 11, 22, 8, 0));
        return l;
    }

    private static LeituraSinais leitura(int fc, int spo2) {
        return new LeituraSinais(fc, spo2, new BigDecimal("36.8"), 120, 80, null);
    }

    @Test
    void leituraNormalAtualizaOPainelSemAlerta() {
        when(leitos.findByCodigo("UTI-01")).thenReturn(Optional.of(leitoOcupado()));

        assertThat(service.registrarLeitura("UTI-01", leitura(80, 97))).isEmpty();

        assertThat(painel.ultima("UTI-01")).isPresent();
        verify(eventos, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void saturacaoCriticaGeraAlertaEEventoParaAsNotificacoes() {
        when(leitos.findByCodigo("UTI-01")).thenReturn(Optional.of(leitoOcupado()));

        List<Alerta> novos = service.registrarLeitura("UTI-01", leitura(80, 85));

        assertThat(novos).hasSize(1);
        assertThat(novos.get(0).getPacienteNome()).isEqualTo("Maria Silva");
        assertThat(novos.get(0).getGravidade()).isEqualTo(GravidadeAlerta.CRITICO);
        verify(eventos).registrar(eq("ALERTA_SINAIS_VITAIS"), eq(1), eq("Maria Silva"), isNull(), anyMap());
    }

    @Test
    void alertaAindaAbertoNaoERepetidoNaProximaLeitura() {
        when(leitos.findByCodigo("UTI-01")).thenReturn(Optional.of(leitoOcupado()));
        Alerta aberto = new Alerta("UTI-01", "UTI", 1, "Maria Silva",
                new Achado(TipoAlerta.SATURACAO_BAIXA, GravidadeAlerta.CRITICO, "85%", "saturação"),
                LocalDateTime.of(2026, 11, 22, 9, 59));
        when(alertas.findFirstByLeitoCodigoAndTipoOrderByCriadoEmDesc("UTI-01", TipoAlerta.SATURACAO_BAIXA))
                .thenReturn(Optional.of(aberto));

        assertThat(service.registrarLeitura("UTI-01", leitura(80, 85))).isEmpty();
        verify(alertas, never()).save(any());
    }

    @Test
    void monitorDeLeitoSemPacienteEIgnorado() {
        when(leitos.findByCodigo("UTI-02")).thenReturn(Optional.of(
                new Leito("UTI-02", Setor.UTI, ENFERMEIRO, LocalDateTime.of(2026, 11, 22, 8, 0))));

        assertThat(service.registrarLeitura("UTI-02", leitura(80, 80))).isEmpty();

        assertThat(painel.ultima("UTI-02")).isEmpty();
        verify(alertas, never()).save(any());
    }

    @Test
    void leitoSemLeituraRecenteApareceSemSinal() {
        Leito l = leitoOcupado();
        when(leitos.findByStatusOrderBySetorAscCodigoAsc(any())).thenReturn(List.of(l));
        painel.registrar("UTI-01", leitura(80, 97), Instant.parse("2026-11-22T09:59:00Z"));

        MonitorLeitoResponse m = service.leitosMonitorados().get(0);

        assertThat(m.semSinal()).isTrue();
        assertThat(m.pacienteNome()).isEqualTo("Maria Silva");
    }

    @Test
    void estadosDeCadaSinal() {
        Map<String, String> estados = MonitoramentoService.estados(new LeituraSinais(120, 85, new BigDecimal("36.5"), 120, 80, null));

        assertThat(estados).containsEntry("saturacao", "CRITICO")
                .containsEntry("frequenciaCardiaca", "ATENCAO")
                .containsEntry("temperatura", "NORMAL")
                .containsEntry("pressao", "NORMAL");
    }

    @Test
    void alertaReconhecidoGuardaQuemViu() {
        Alerta a = new Alerta("UTI-01", "UTI", 1, "Maria Silva",
                new Achado(TipoAlerta.FEBRE, GravidadeAlerta.ATENCAO, "38,2 °C", "febre"), LocalDateTime.of(2026, 11, 22, 9, 0));
        when(alertas.findById(5)).thenReturn(Optional.of(a));

        service.reconhecer(5, ENFERMEIRO);

        assertThat(a.ativo()).isFalse();
        assertThat(a.getReconhecidoPorNome()).isEqualTo("Enf. Bruno");
        verify(alertas, times(1)).save(a);
    }
}
