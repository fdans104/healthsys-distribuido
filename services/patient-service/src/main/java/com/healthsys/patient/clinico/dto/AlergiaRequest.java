package com.healthsys.patient.clinico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do POST /api/v1/pacientes/{id}/alergias. */
public record AlergiaRequest(
        @NotBlank(message = "descricao é obrigatória (ex.: Dipirona, Penicilina, Frutos do mar)")
        @Size(max = 150, message = "descricao deve ter no máximo 150 caracteres")
        String descricao
) {
}
