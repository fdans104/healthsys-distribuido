package com.healthsys.triage.triagem.dto;

import com.healthsys.triage.triagem.ClassificacaoRisco;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Corpo do PUT /api/v1/triagens/{id}/avaliacao: a enfermagem avalia e classifica o risco (RF14, RF15). */
public record AvaliacaoRequest(
        @NotBlank(message = "sintomas são obrigatórios")
        @Size(max = 4000, message = "sintomas deve ter no máximo 4000 caracteres")
        String sintomas,

        @Pattern(regexp = "^\\d{2,3}/\\d{2,3}$", message = "pressão no formato 120/80")
        String pressaoArterial,

        @DecimalMin(value = "30.0", message = "temperatura deve estar entre 30 e 45 °C")
        @DecimalMax(value = "45.0", message = "temperatura deve estar entre 30 e 45 °C")
        BigDecimal temperatura,

        @Min(value = 20, message = "frequência cardíaca deve estar entre 20 e 250 bpm")
        @Max(value = 250, message = "frequência cardíaca deve estar entre 20 e 250 bpm")
        Integer frequenciaCardiaca,

        @Min(value = 50, message = "saturação deve estar entre 50 e 100%")
        @Max(value = 100, message = "saturação deve estar entre 50 e 100%")
        Integer saturacao,

        @NotNull(message = "classificacaoRisco é obrigatória (BAIXO, MEDIO, ALTO ou CRITICO)")
        ClassificacaoRisco classificacaoRisco,

        @NotNull(message = "versao é obrigatória")
        Long versao
) {
}
