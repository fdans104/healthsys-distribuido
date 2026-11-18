package com.healthsys.patient.clinico;

import com.healthsys.patient.clinico.dto.AlergiaRequest;
import com.healthsys.patient.clinico.dto.AlergiaResponse;
import com.healthsys.patient.clinico.dto.AtendimentoRequest;
import com.healthsys.patient.clinico.dto.AtendimentoResponse;
import com.healthsys.patient.clinico.dto.HistoricoItem;
import com.healthsys.patient.clinico.dto.VacinaRequest;
import com.healthsys.patient.clinico.dto.VacinaResponse;
import com.healthsys.patient.exception.RecursoNaoEncontradoException;
import com.healthsys.patient.paciente.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicoServiceTest {

    private static final Profissional DRA_ANA = new Profissional(7, "Dra. Ana", "MEDICO");
    private static final Profissional ENF_BRUNO = new Profissional(8, "Enf. Bruno", "ENFERMEIRO");

    private PacienteRepository pacientes;
    private AlergiaRepository alergias;
    private VacinaRepository vacinas;
    private AtendimentoRepository atendimentos;
    private ClinicoService service;

    @BeforeEach
    void setUp() {
        pacientes = mock(PacienteRepository.class);
        alergias = mock(AlergiaRepository.class);
        vacinas = mock(VacinaRepository.class);
        atendimentos = mock(AtendimentoRepository.class);
        service = new ClinicoService(pacientes, alergias, vacinas, atendimentos);

        when(pacientes.existsById(1)).thenReturn(true);
        when(alergias.save(any(Alergia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vacinas.save(any(Vacina.class))).thenAnswer(inv -> inv.getArgument(0));
        when(atendimentos.save(any(Atendimento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void alergiaFicaVinculadaAoPacienteEGuardaQuemRegistrou() {
        AlergiaResponse resp = service.registrarAlergia(1, new AlergiaRequest("  Dipirona  "), DRA_ANA);

        assertThat(resp.pacienteId()).isEqualTo(1);
        assertThat(resp.descricao()).isEqualTo("Dipirona");
        assertThat(resp.registradoPorNome()).isEqualTo("Dra. Ana");
        assertThat(resp.registradoPorPerfil()).isEqualTo("MEDICO");
    }

    @Test
    void vacinaGuardaDoseLoteEQuemAplicou() {
        VacinaResponse resp = service.registrarVacina(1,
                new VacinaRequest("Hepatite B", LocalDate.of(2026, 3, 10), "1ª dose", "  "), ENF_BRUNO);

        assertThat(resp.nomeVacina()).isEqualTo("Hepatite B");
        assertThat(resp.dose()).isEqualTo("1ª dose");
        assertThat(resp.lote()).isNull();
        assertThat(resp.registradoPorNome()).isEqualTo("Enf. Bruno");
    }

    @Test
    void atendimentoGuardaTipoDataEObservacoes() {
        LocalDateTime quando = LocalDateTime.of(2026, 11, 10, 9, 30);
        AtendimentoResponse resp = service.registrarAtendimento(1,
                new AtendimentoRequest(TipoAtendimento.CONSULTA, quando, "Dor de cabeça há 3 dias."), DRA_ANA);

        assertThat(resp.tipo()).isEqualTo(TipoAtendimento.CONSULTA);
        assertThat(resp.data()).isEqualTo(quando);
        assertThat(resp.observacoes()).isEqualTo("Dor de cabeça há 3 dias.");
    }

    @Test
    void naoRegistraNadaParaPacienteInexistente() {
        when(pacientes.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> service.registrarAlergia(99, new AlergiaRequest("Dipirona"), DRA_ANA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.historico(99)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(alergias, never()).save(any());
    }

    @Test
    void historicoJuntaTudoDoMaisRecenteParaOMaisAntigo() {
        Atendimento consulta = new Atendimento(1, TipoAtendimento.CONSULTA,
                LocalDateTime.of(2026, 11, 10, 9, 0), "Retorno em 30 dias", DRA_ANA);
        Vacina vacina = new Vacina(1, "Tétano", LocalDate.of(2026, 1, 5), null, "AB123", ENF_BRUNO);
        Alergia alergia = new Alergia(1, "Penicilina", DRA_ANA);
        ReflectionTestUtils.setField(alergia, "registradoEm", LocalDateTime.of(2026, 6, 1, 14, 0));

        when(atendimentos.findByPacienteIdOrderByDataDescIdDesc(1)).thenReturn(List.of(consulta));
        when(vacinas.findByPacienteIdOrderByDataAplicacaoDescIdDesc(1)).thenReturn(List.of(vacina));
        when(alergias.findByPacienteIdOrderByRegistradoEmDescIdDesc(1)).thenReturn(List.of(alergia));

        List<HistoricoItem> historico = service.historico(1);

        assertThat(historico).extracting(HistoricoItem::categoria)
                .containsExactly("ATENDIMENTO", "ALERGIA", "VACINA");
        assertThat(historico.get(2).detalhe()).isEqualTo("lote AB123");
    }

    @Test
    void historicoVazioQuandoNaoHaRegistros() {
        assertThat(service.historico(1)).isEmpty();
    }

    @Test
    void sabeSeOPacienteTemProntuario() {
        when(vacinas.existsByPacienteId(1)).thenReturn(true);

        assertThat(service.possuiRegistrosClinicos(1)).isTrue();
        assertThat(service.possuiRegistrosClinicos(2)).isFalse();
    }
}
