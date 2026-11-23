package com.healthsys.record.prontuario;

import com.healthsys.record.eventos.RegistroDeEventos;
import com.healthsys.record.exception.RecursoNaoEncontradoException;
import com.healthsys.record.exception.RegraNegocioException;
import com.healthsys.record.paciente.PacienteClient;
import com.healthsys.record.paciente.PacienteResumo;
import com.healthsys.record.prontuario.dto.ConsultaRequest;
import com.healthsys.record.prontuario.dto.ExameRequest;
import com.healthsys.record.prontuario.dto.MedicamentoRequest;
import com.healthsys.record.prontuario.dto.ResultadoExameRequest;
import com.healthsys.record.prontuario.dto.SuspensaoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

class ProntuarioServiceTest {

    private static final String TOKEN = "Bearer abc";
    private static final Profissional MEDICA = new Profissional(7, "Dra. Ana", "MEDICO");
    private static final Profissional ENFERMEIRO = new Profissional(8, "Enf. Bruno", "ENFERMEIRO");
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneId.of("UTC"));

    private ConsultaRepository consultas;
    private ExameRepository exames;
    private MedicamentoRepository medicamentos;
    private PacienteClient pacientes;
    private RegistroDeEventos eventos;
    private ProntuarioService service;

    @BeforeEach
    void setUp() {
        consultas = mock(ConsultaRepository.class);
        exames = mock(ExameRepository.class);
        medicamentos = mock(MedicamentoRepository.class);
        pacientes = mock(PacienteClient.class);
        eventos = mock(RegistroDeEventos.class);
        service = new ProntuarioService(consultas, exames, medicamentos, pacientes, eventos, RELOGIO);

        when(pacientes.buscar(1, TOKEN)).thenReturn(new PacienteResumo(1, "Maria"));
        when(consultas.save(any(Consulta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(exames.save(any(Exame.class))).thenAnswer(inv -> inv.getArgument(0));
        when(medicamentos.save(any(Medicamento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void consultaGuardaOMedicoEQuandoFoiRegistrada() {
        Consulta c = service.registrarConsulta(1, new ConsultaRequest(LocalDateTime.of(2026, 11, 16, 9, 0),
                "  Febre há 2 dias ", null, "Virose", "Hidratação"), MEDICA, TOKEN);

        assertThat(c.getMotivo()).isEqualTo("Febre há 2 dias");
        assertThat(c.getAvaliacao()).isNull();
        assertThat(c.getRegistradoPorNome()).isEqualTo("Dra. Ana");
        assertThat(c.getRegistradoEm()).isEqualTo(LocalDateTime.of(2026, 11, 16, 12, 0));
    }

    @Test
    void naoRegistraNadaParaPacienteQueNaoExiste() {
        when(pacientes.buscar(99, TOKEN)).thenThrow(new RecursoNaoEncontradoException("Paciente 99 não encontrado"));

        assertThatThrownBy(() -> service.solicitarExame(99, new ExameRequest("Hemograma", TipoExame.LABORATORIAL, null),
                MEDICA, TOKEN)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(exames, never()).save(any());
    }

    @Test
    void exameComecaSolicitadoEFicaConcluidoComOResultado() {
        Exame exame = service.solicitarExame(1, new ExameRequest("Hemograma", TipoExame.LABORATORIAL, null), MEDICA, TOKEN);
        assertThat(exame.getStatus()).isEqualTo(StatusExame.SOLICITADO);

        when(exames.findByIdAndPacienteId("e1", 1)).thenReturn(Optional.of(exame));
        Exame concluido = service.registrarResultado(1, "e1",
                new ResultadoExameRequest("Sem alterações", LocalDate.of(2026, 11, 16)), ENFERMEIRO);

        assertThat(concluido.getStatus()).isEqualTo(StatusExame.CONCLUIDO);
        assertThat(concluido.getResultadoRegistradoPorNome()).isEqualTo("Enf. Bruno");
    }

    @Test
    void resultadoDeExameNaoPodeSerAlterado() {
        Exame exame = new Exame(1, "Raio-X", TipoExame.IMAGEM, null, MEDICA, LocalDateTime.now());
        exame.registrarResultado("Normal", LocalDate.of(2026, 11, 1), MEDICA, LocalDateTime.now());
        when(exames.findByIdAndPacienteId("e1", 1)).thenReturn(Optional.of(exame));

        assertThatThrownBy(() -> service.registrarResultado(1, "e1",
                new ResultadoExameRequest("Outro", LocalDate.of(2026, 11, 2)), MEDICA))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void exameDeOutroPacienteNaoEhEncontrado() {
        when(exames.findByIdAndPacienteId("e1", 2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarResultado(2, "e1",
                new ResultadoExameRequest("Normal", LocalDate.of(2026, 11, 2)), MEDICA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void medicamentoComDataFimAntesDoInicioEhRecusado() {
        MedicamentoRequest req = new MedicamentoRequest("Dipirona", "500 mg", ViaAdministracao.ORAL, "de 6 em 6 horas",
                LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 10), null);

        assertThatThrownBy(() -> service.prescrever(1, req, MEDICA, TOKEN)).isInstanceOf(RegraNegocioException.class);
        verify(medicamentos, never()).save(any());
    }

    @Test
    void medicamentoSuspensoGuardaOMotivoENaoSuspendeDuasVezes() {
        Medicamento m = service.prescrever(1, new MedicamentoRequest("Amoxicilina", "500 mg", ViaAdministracao.ORAL,
                "de 8 em 8 horas", LocalDate.of(2026, 11, 16), null, null), MEDICA, TOKEN);
        assertThat(m.getStatus()).isEqualTo(StatusMedicamento.ATIVO);

        when(medicamentos.findByIdAndPacienteId("m1", 1)).thenReturn(Optional.of(m));
        Medicamento suspenso = service.suspender(1, "m1", new SuspensaoRequest("Reação alérgica"), MEDICA);

        assertThat(suspenso.getStatus()).isEqualTo(StatusMedicamento.SUSPENSO);
        assertThat(suspenso.getMotivoSuspensao()).isEqualTo("Reação alérgica");
        assertThatThrownBy(() -> service.suspender(1, "m1", new SuspensaoRequest("De novo"), MEDICA))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void registrosGeramEventosParaAsNotificacoes() {
        Exame exame = service.solicitarExame(1, new ExameRequest("Hemograma", TipoExame.LABORATORIAL, null), MEDICA, TOKEN);
        verify(eventos).registrar(eq("EXAME_SOLICITADO"), eq(1), eq("Maria"), eq(MEDICA), anyMap());
        assertThat(exame.getPacienteNome()).isEqualTo("Maria");

        when(exames.findByIdAndPacienteId("e1", 1)).thenReturn(Optional.of(exame));
        service.registrarResultado(1, "e1", new ResultadoExameRequest("Normal", LocalDate.of(2026, 11, 16)), ENFERMEIRO);
        verify(eventos).registrar(eq("EXAME_RESULTADO_REGISTRADO"), eq(1), eq("Maria"), eq(ENFERMEIRO), anyMap());

        service.prescrever(1, new MedicamentoRequest("Dipirona", "500 mg", ViaAdministracao.ORAL, "de 6 em 6 horas",
                LocalDate.of(2026, 11, 16), null, null), MEDICA, TOKEN);
        verify(eventos).registrar(eq("MEDICAMENTO_PRESCRITO"), eq(1), eq("Maria"), eq(MEDICA), anyMap());
    }

    @Test
    void semPacienteNaoGeraEvento() {
        when(pacientes.buscar(99, TOKEN)).thenThrow(new RecursoNaoEncontradoException("Paciente 99 não encontrado"));

        assertThatThrownBy(() -> service.solicitarExame(99, new ExameRequest("Hemograma", TipoExame.LABORATORIAL, null),
                MEDICA, TOKEN)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(eventos, never()).registrar(any(), any(), any(), any(), any());
    }
}
