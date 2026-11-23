package com.healthsys.monitoring.leito;

import com.healthsys.monitoring.eventos.RegistroDeEventos;
import com.healthsys.monitoring.exception.ConflitoEdicaoException;
import com.healthsys.monitoring.exception.RegraNegocioException;
import com.healthsys.monitoring.exception.ServicoIndisponivelException;
import com.healthsys.monitoring.leito.dto.LeitoRequest;
import com.healthsys.monitoring.leito.dto.LeitoResponse;
import com.healthsys.monitoring.leito.dto.OcupacaoRequest;
import com.healthsys.monitoring.leito.dto.OcupacaoSetorResponse;
import com.healthsys.monitoring.paciente.PacienteClient;
import com.healthsys.monitoring.paciente.PacienteResumo;
import com.healthsys.monitoring.sinais.AlertaRepository;
import com.healthsys.monitoring.sinais.PainelDeLeituras;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeitoServiceTest {

    private static final String TOKEN = "Bearer abc";
    private static final Profissional MEDICA = new Profissional(7, "Dra. Ana", "MEDICO");
    private static final Profissional ENFERMEIRO = new Profissional(8, "Enf. Bruno", "ENFERMEIRO");
    private static final Profissional ADMIN = new Profissional(1, "Administrador", "ADMIN");
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC);

    private LeitoRepository leitos;
    private AlertaRepository alertas;
    private PacienteClient pacientes;
    private RegistroDeEventos eventos;
    private LeitoService service;

    @BeforeEach
    void setUp() {
        leitos = mock(LeitoRepository.class);
        alertas = mock(AlertaRepository.class);
        pacientes = mock(PacienteClient.class);
        eventos = mock(RegistroDeEventos.class);
        service = new LeitoService(leitos, alertas, new PainelDeLeituras(), pacientes, eventos, RELOGIO);
        when(leitos.saveAndFlush(any(Leito.class))).thenAnswer(inv -> inv.getArgument(0));
        when(leitos.save(any(Leito.class))).thenAnswer(inv -> inv.getArgument(0));
        when(leitos.findByPacienteId(any())).thenReturn(Optional.empty());
        when(pacientes.buscar(1, TOKEN)).thenReturn(new PacienteResumo(1, "Maria Silva"));
    }

    private Leito naBase(StatusLeito status) {
        Leito l = new Leito("UTI-01", Setor.UTI, ADMIN, LocalDateTime.of(2026, 11, 22, 8, 0));
        ReflectionTestUtils.setField(l, "id", 10);
        ReflectionTestUtils.setField(l, "versao", 0L);
        if (status == StatusLeito.OCUPADO) {
            l.ocupar(1, "Maria Silva", MEDICA, LocalDateTime.of(2026, 11, 22, 8, 0));
        } else if (status == StatusLeito.HIGIENIZACAO) {
            l.ocupar(1, "Maria Silva", MEDICA, LocalDateTime.of(2026, 11, 22, 8, 0));
            l.liberar(MEDICA, LocalDateTime.of(2026, 11, 22, 9, 0));
        }
        when(leitos.findById(10)).thenReturn(Optional.of(l));
        return l;
    }

    @Test
    void internaPacienteEmLeitoLivre() {
        naBase(StatusLeito.LIVRE);

        LeitoResponse r = service.ocupar(10, new OcupacaoRequest(1, 0L), MEDICA, TOKEN);

        assertThat(r.status()).isEqualTo(StatusLeito.OCUPADO);
        assertThat(r.pacienteNome()).isEqualTo("Maria Silva");
        assertThat(r.ocupadoPorNome()).isEqualTo("Dra. Ana");
        verify(eventos).registrar(eq("LEITO_OCUPADO"), eq(1), eq("Maria Silva"), eq(MEDICA), anyMap());
    }

    @Test
    void naoInternaEmLeitoOcupadoOuEmHigienizacao() {
        naBase(StatusLeito.OCUPADO);
        assertThatThrownBy(() -> service.ocupar(10, new OcupacaoRequest(1, 0L), MEDICA, TOKEN))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("ocupado");

        naBase(StatusLeito.HIGIENIZACAO);
        assertThatThrownBy(() -> service.ocupar(10, new OcupacaoRequest(1, 0L), MEDICA, TOKEN))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("higienização");
    }

    @Test
    void pacienteNaoOcupaDoisLeitos() {
        naBase(StatusLeito.LIVRE);
        Leito outro = new Leito("ENF-02", Setor.ENFERMARIA, ADMIN, LocalDateTime.of(2026, 11, 22, 8, 0));
        when(leitos.findByPacienteId(1)).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> service.ocupar(10, new OcupacaoRequest(1, 0L), MEDICA, TOKEN))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("ENF-02");
    }

    @Test
    void versaoAntigaERecusada() {
        naBase(StatusLeito.LIVRE);
        assertThatThrownBy(() -> service.ocupar(10, new OcupacaoRequest(1, 5L), MEDICA, TOKEN))
                .isInstanceOf(ConflitoEdicaoException.class);
    }

    @Test
    void comOServicoDePacientesForaDoArNadaEGravado() {
        naBase(StatusLeito.LIVRE);
        when(pacientes.buscar(1, TOKEN)).thenThrow(new ServicoIndisponivelException("fora do ar"));

        assertThatThrownBy(() -> service.ocupar(10, new OcupacaoRequest(1, 0L), MEDICA, TOKEN))
                .isInstanceOf(ServicoIndisponivelException.class);
        verify(leitos, never()).saveAndFlush(any());
        verify(eventos, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void altaEncerraOsAlertasEMandaParaHigienizacao() {
        naBase(StatusLeito.OCUPADO);

        LeitoResponse r = service.liberar(10, 0L, ENFERMEIRO);

        assertThat(r.status()).isEqualTo(StatusLeito.HIGIENIZACAO);
        assertThat(r.pacienteId()).isNull();
        verify(alertas).encerrarAtivosDoLeito(eq("UTI-01"), any(), any());
        verify(eventos).registrar(eq("LEITO_LIBERADO"), eq(1), eq("Maria Silva"), eq(ENFERMEIRO), anyMap());
    }

    @Test
    void enfermagemConfirmaAHigienizacaoEOLeitoFicaLivre() {
        naBase(StatusLeito.HIGIENIZACAO);

        assertThat(service.marcarHigienizado(10, 0L, ENFERMEIRO).status()).isEqualTo(StatusLeito.LIVRE);
    }

    @Test
    void soRemoveLeitoLivreENaoRepeteCodigo() {
        naBase(StatusLeito.OCUPADO);
        assertThatThrownBy(() -> service.remover(10)).isInstanceOf(RegraNegocioException.class);

        when(leitos.existsByCodigo("UTI-09")).thenReturn(true);
        assertThatThrownBy(() -> service.criar(new LeitoRequest(" uti-09 ", Setor.UTI), ADMIN))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("UTI-09");
    }

    @Test
    void ocupacaoPorSetorCalculaATaxa() {
        when(leitos.countBySetorAndStatus(any(), any())).thenReturn(0L);
        when(leitos.countBySetorAndStatus(Setor.UTI, StatusLeito.OCUPADO)).thenReturn(3L);
        when(leitos.countBySetorAndStatus(Setor.UTI, StatusLeito.LIVRE)).thenReturn(1L);

        List<OcupacaoSetorResponse> ocupacao = service.ocupacao();

        OcupacaoSetorResponse uti = ocupacao.stream().filter(o -> o.setor() == Setor.UTI).findFirst().orElseThrow();
        assertThat(uti.total()).isEqualTo(4);
        assertThat(uti.taxaOcupacao()).isEqualTo(75);
    }
}
