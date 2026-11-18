package com.healthsys.triage.triagem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo do POST /api/v1/triagens: o paciente chegou e entra na fila da triagem. */
public record AberturaRequest(
        @NotNull(message = "pacienteId é obrigatório")
        Integer pacienteId,

        @NotBlank(message = "queixaPrincipal é obrigatória (o que trouxe o paciente)")
        @Size(max = 500, message = "queixaPrincipal deve ter no máximo 500 caracteres")
        String queixaPrincipal
) {
}
