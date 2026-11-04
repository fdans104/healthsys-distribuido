package com.healthsys.patient.paciente.dto;

import com.healthsys.patient.paciente.Sexo;
import com.healthsys.patient.validacao.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Corpo do POST e do PUT de /api/v1/pacientes. */
public record PacienteRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "cpf é obrigatório")
        @Cpf
        String cpf,

        @NotNull(message = "dataNascimento é obrigatória (formato AAAA-MM-DD)")
        @PastOrPresent(message = "dataNascimento não pode estar no futuro")
        LocalDate dataNascimento,

        @NotNull(message = "sexo é obrigatório (M, F ou O)")
        Sexo sexo,

        @Size(max = 20, message = "telefone deve ter no máximo 20 caracteres")
        String telefone,

        @Email(message = "email inválido")
        @Size(max = 150, message = "email deve ter no máximo 150 caracteres")
        String email,

        @Size(max = 255, message = "endereco deve ter no máximo 255 caracteres")
        String endereco
) {
}
