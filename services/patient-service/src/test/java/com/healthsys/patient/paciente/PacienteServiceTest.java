package com.healthsys.patient.paciente;

import com.healthsys.patient.exception.CpfJaCadastradoException;
import com.healthsys.patient.exception.RecursoNaoEncontradoException;
import com.healthsys.patient.paciente.dto.PacienteRequest;
import com.healthsys.patient.paciente.dto.PacienteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PacienteServiceTest {

    private PacienteRepository repository;
    private PacienteService service;

    @BeforeEach
    void setUp() {
        repository = mock(PacienteRepository.class);
        service = new PacienteService(repository);
        when(repository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveAndFlush(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static PacienteRequest requisicao(String cpf, String email) {
        return new PacienteRequest("  Maria Silva  ", cpf, LocalDate.of(1990, 5, 15), Sexo.F,
                " (85) 98888-0001 ", email, "   ");
    }

    @Test
    void criarNormalizaCpfEmailECamposVazios() {
        PacienteResponse resp = service.criar(requisicao("529.982.247-25", " Maria@Email.COM "));

        ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
        verify(repository).save(captor.capture());
        Paciente salvo = captor.getValue();

        assertThat(salvo.getNome()).isEqualTo("Maria Silva");
        assertThat(salvo.getCpf()).isEqualTo("52998224725");
        assertThat(salvo.getEmail()).isEqualTo("maria@email.com");
        assertThat(salvo.getTelefone()).isEqualTo("(85) 98888-0001");
        assertThat(salvo.getEndereco()).isNull();
        assertThat(resp.idade()).isGreaterThanOrEqualTo(36);
    }

    @Test
    void criarRejeitaCpfDuplicado() {
        when(repository.existsByCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(requisicao("529.982.247-25", null)))
                .isInstanceOf(CpfJaCadastradoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarRejeitaCpfDeOutroPaciente() {
        Paciente existente = new Paciente("Maria", "11144477735", LocalDate.of(1990, 5, 15), Sexo.F, null, null, null);
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByCpfAndIdNot("52998224725", 1)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1, requisicao("52998224725", null)))
                .isInstanceOf(CpfJaCadastradoException.class);
    }

    @Test
    void buscaSoComNumerosUsaCpfEComTextoUsaNome() {
        when(repository.findByCpfStartingWithOrderByNomeAsc(anyString())).thenReturn(List.of());
        when(repository.findByNomeContainingIgnoreCaseOrderByNomeAsc(anyString())).thenReturn(List.of());

        service.listar("529.982");
        service.listar(" maria ");

        verify(repository).findByCpfStartingWithOrderByNomeAsc("529982");
        verify(repository).findByNomeContainingIgnoreCaseOrderByNomeAsc("maria");
    }

    @Test
    void buscarInexistenteLancaNaoEncontrado() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
