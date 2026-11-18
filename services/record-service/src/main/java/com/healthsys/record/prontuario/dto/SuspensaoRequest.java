package com.healthsys.record.prontuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do PUT /api/v1/prontuarios/{pacienteId}/medicamentos/{medicamentoId}/suspensao. */
public record SuspensaoRequest(
        @NotBlank(message = "motivo da suspensão é obrigatório")
        @Size(max = 500, message = "motivo deve ter no máximo 500 caracteres")
        String motivo
) {
}
