package com.healthsys.patient.clinico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Corpo do POST /api/v1/pacientes/{id}/vacinas (RN22: nome e data de aplicação são obrigatórios). */
public record VacinaRequest(
        @NotBlank(message = "nomeVacina é obrigatório")
        @Size(max = 100, message = "nomeVacina deve ter no máximo 100 caracteres")
        String nomeVacina,

        @NotNull(message = "dataAplicacao é obrigatória (formato AAAA-MM-DD)")
        @PastOrPresent(message = "dataAplicacao não pode estar no futuro")
        LocalDate dataAplicacao,

        @Size(max = 30, message = "dose deve ter no máximo 30 caracteres")
        String dose,

        @Size(max = 30, message = "lote deve ter no máximo 30 caracteres")
        String lote
) {
}
