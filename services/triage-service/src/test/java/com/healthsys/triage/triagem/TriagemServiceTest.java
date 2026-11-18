package com.healthsys.triage.triagem;

import com.healthsys.triage.exception.AcaoNaoPermitidaException;
import com.healthsys.triage.exception.ConflitoEdicaoException;
import com.healthsys.triage.exception.RegraNegocioException;
import com.healthsys.triage.paciente.PacienteClient;
import com.healthsys.triage.paciente.PacienteResumo;
import com.healthsys.triage.triagem.dto.AberturaRequest;
import com.healthsys.triage.triagem.dto.AvaliacaoRequest;
import com.healthsys.triage.triagem.dto.StatusRequest;
import com.healthsys.triage.triagem.dto.TriagemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriagemServiceTest {

    private static final String TOKEN = "Bearer abc";
    private static final Profissional RECEPCAO = new Profissional(5, "Carla", "RECEPCIONISTA");
    private static final Profissional ENFERMEIRO = new Profissional(8, "Enf. Bruno", "ENFERMEIRO");
    private static final Profissional MEDICA = new Profissional(7, "Dra. Ana", "MEDICO");
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneId.of("UTC"));

    private TriagemRepository repository;
    private PacienteClient pacientes;
    private TriagemService service;

    @BeforeEach
    void setUp() {
        repository = mock(TriagemRepository.class);
        pacientes = mock(PacienteClient.class);
        service = new TriagemService(repository, pacientes, RELOGIO);
        when(pacientes.buscar(1, TOKEN)).thenReturn(new PacienteResumo(1, "Maria Silva"));
        when(repository.save(any(Triagem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveAndFlush(any(Triagem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Triagem gravada, no status, risco e versão informados (no sistema real quem preenche a versão é o JPA). */
    private static Triagem triagem(StatusTriagem status, ClassificacaoRisco risco, long versao) {
        Triagem t = new Triagem(1, "Maria Silva", "Dor no peito", RECEPCAO, LocalDateTime.of(2026, 11, 16, 8, 0));
        ReflectionTestUtils.setField(t, "status", status);
        ReflectionTestUtils.setField(t, "classificacaoRisco", risco);
        ReflectionTestUtils.setField(t, "versao", versao);
        return t;
    }

    private void naBase(Triagem t) {
        when(repository.findById(10)).thenReturn(Optional.of(t));
    }

    private static AvaliacaoRequest avaliacao(ClassificacaoRisco risco, long versao) {
        return new AvaliacaoRequest("Dor no peito há 1 hora", "150/95", new BigDecimal("36.8"), 110, 96, risco, versao);
    }

    @Test
    void recepcaoAbreTriagemPendenteComONomeDoPaciente() {
        TriagemResponse resp = service.abrir(new AberturaRequest(1, "  Dor no peito "), RECEPCAO, TOKEN);

        assertThat(resp.status()).isEqualTo(StatusTriagem.PENDENTE);
        assertThat(resp.pacienteNome()).isEqualTo("Maria Silva");
        assertThat(resp.queixaPrincipal()).isEqualTo("Dor no peito");
        assertThat(resp.classificacaoRisco()).isNull();
    }

    @Test
    void naoAbreSegundaTriagemParaQuemJaEstaNaFila() {
        when(repository.existsByPacienteIdAndStatusIn(eq(1), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.abrir(new AberturaRequest(1, "Febre"), RECEPCAO, TOKEN))
                .isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void enfermagemClassificaORisco() {
        naBase(triagem(StatusTriagem.PENDENTE, null, 0));

        TriagemResponse resp = service.avaliar(10, avaliacao(ClassificacaoRisco.ALTO, 0), ENFERMEIRO);

        assertThat(resp.classificacaoRisco()).isEqualTo(ClassificacaoRisco.ALTO);
        assertThat(resp.avaliadaPorNome()).isEqualTo("Enf. Bruno");
    }

    @Test
    void avaliacaoComVersaoAntigaEhRecusada() {
        naBase(triagem(StatusTriagem.PENDENTE, null, 2));

        assertThatThrownBy(() -> service.avaliar(10, avaliacao(ClassificacaoRisco.ALTO, 1), ENFERMEIRO))
                .isInstanceOf(ConflitoEdicaoException.class);
    }

    @Test
    void semClassificacaoDeRiscoNaoHaAtendimento() {
        naBase(triagem(StatusTriagem.PENDENTE, null, 0));

        assertThatThrownBy(() -> service.mudarStatus(10, new StatusRequest(StatusTriagem.EM_ATENDIMENTO, 0L), MEDICA))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void medicoAtendeEFinaliza() {
        Triagem t = triagem(StatusTriagem.PENDENTE, ClassificacaoRisco.MEDIO, 0);
        naBase(t);

        service.mudarStatus(10, new StatusRequest(StatusTriagem.EM_ATENDIMENTO, 0L), MEDICA);
        assertThat(t.getStatus()).isEqualTo(StatusTriagem.EM_ATENDIMENTO);

        service.mudarStatus(10, new StatusRequest(StatusTriagem.FINALIZADA, 0L), MEDICA);
        assertThat(t.getStatus()).isEqualTo(StatusTriagem.FINALIZADA);
    }

    @Test
    void naoFinalizaSemPassarPeloAtendimento() {
        naBase(triagem(StatusTriagem.PENDENTE, ClassificacaoRisco.BAIXO, 0));

        assertThatThrownBy(() -> service.mudarStatus(10, new StatusRequest(StatusTriagem.FINALIZADA, 0L), MEDICA))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void triagemFinalizadaNaoVoltaParaAFila() {
        naBase(triagem(StatusTriagem.FINALIZADA, ClassificacaoRisco.BAIXO, 0));

        assertThatThrownBy(() -> service.mudarStatus(10, new StatusRequest(StatusTriagem.PENDENTE, 0L), MEDICA))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.mudarStatus(10, new StatusRequest(StatusTriagem.EM_ATENDIMENTO, 0L), MEDICA))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void soOMedicoChamaParaAtendimento() {
        naBase(triagem(StatusTriagem.PENDENTE, ClassificacaoRisco.ALTO, 0));

        assertThatThrownBy(() -> service.mudarStatus(10, new StatusRequest(StatusTriagem.EM_ATENDIMENTO, 0L), ENFERMEIRO))
                .isInstanceOf(AcaoNaoPermitidaException.class);
    }

    @Test
    void recepcaoPodeCancelarQuandoOPacienteDesiste() {
        Triagem t = triagem(StatusTriagem.PENDENTE, null, 0);
        naBase(t);

        service.mudarStatus(10, new StatusRequest(StatusTriagem.CANCELADA, 0L), RECEPCAO);

        assertThat(t.getStatus()).isEqualTo(StatusTriagem.CANCELADA);
    }

    @Test
    void filaComecaPeloAtendimentoDepoisPeloRiscoMaisGrave() {
        Triagem baixo = triagem(StatusTriagem.PENDENTE, ClassificacaoRisco.BAIXO, 0);
        Triagem critico = triagem(StatusTriagem.PENDENTE, ClassificacaoRisco.CRITICO, 0);
        Triagem semRisco = triagem(StatusTriagem.PENDENTE, null, 0);
        Triagem atendendo = triagem(StatusTriagem.EM_ATENDIMENTO, ClassificacaoRisco.MEDIO, 0);
        Triagem encerrada = triagem(StatusTriagem.FINALIZADA, ClassificacaoRisco.ALTO, 0);
        when(repository.findAll()).thenReturn(List.of(baixo, encerrada, critico, atendendo, semRisco));

        List<TriagemResponse> fila = service.listar(null, null);

        assertThat(fila).extracting(TriagemResponse::classificacaoRisco).containsExactly(
                ClassificacaoRisco.MEDIO, null, ClassificacaoRisco.CRITICO, ClassificacaoRisco.BAIXO, ClassificacaoRisco.ALTO);
    }
}
