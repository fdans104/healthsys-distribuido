package com.healthsys.record.prontuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Corpo do POST /api/v1/prontuarios/{pacienteId}/consultas (RN28: a data da consulta é obrigatória). */
public record ConsultaRequest(
        @NotNull(message = "data é obrigatória (formato AAAA-MM-DDTHH:MM)")
        @PastOrPresent(message = "data não pode estar no futuro")
        LocalDateTime data,

        @NotBlank(message = "motivo é obrigatório (a queixa que trouxe o paciente)")
        @Size(max = 500, message = "motivo deve ter no máximo 500 caracteres")
        String motivo,

        @Size(max = 4000, message = "avaliacao deve ter no máximo 4000 caracteres")
        String avaliacao,

        @Size(max = 500, message = "diagnostico deve ter no máximo 500 caracteres")
        String diagnostico,

        @Size(max = 4000, message = "conduta deve ter no máximo 4000 caracteres")
        String conduta
) {
}
