package com.healthsys.patient.paciente.dto;

import com.healthsys.patient.paciente.Paciente;
import com.healthsys.patient.paciente.Sexo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/** O que a API devolve. A idade é calculada a partir da data de nascimento. */
public record PacienteResponse(
        Integer id,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        Integer idade,
        Sexo sexo,
        String telefone,
        String email,
        String endereco,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static PacienteResponse de(Paciente p) {
        Integer idade = p.getDataNascimento() == null
                ? null
                : Period.between(p.getDataNascimento(), LocalDate.now()).getYears();
        return new PacienteResponse(
                p.getId(), p.getNome(), p.getCpf(), p.getDataNascimento(), idade, p.getSexo(),
                p.getTelefone(), p.getEmail(), p.getEndereco(), p.getCriadoEm(), p.getAtualizadoEm());
    }
}
