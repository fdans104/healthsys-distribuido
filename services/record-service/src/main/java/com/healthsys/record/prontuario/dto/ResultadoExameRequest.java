package com.healthsys.record.prontuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Corpo do PUT /api/v1/prontuarios/{pacienteId}/exames/{exameId}/resultado. */
public record ResultadoExameRequest(
        @NotBlank(message = "resultado é obrigatório")
        @Size(max = 4000, message = "resultado deve ter no máximo 4000 caracteres")
        String resultado,

        @NotNull(message = "dataResultado é obrigatória (formato AAAA-MM-DD)")
        @PastOrPresent(message = "dataResultado não pode estar no futuro")
        LocalDate dataResultado
) {
}
