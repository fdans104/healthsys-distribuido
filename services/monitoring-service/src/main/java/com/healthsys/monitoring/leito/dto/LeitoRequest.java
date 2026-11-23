package com.healthsys.monitoring.leito.dto;

import com.healthsys.monitoring.leito.Setor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Corpo do POST /api/v1/leitos (o administrador cadastra um leito). */
public record LeitoRequest(
        @NotBlank(message = "codigo é obrigatório (ex.: UTI-05)")
        @Pattern(regexp = "[A-Za-z0-9-]{2,20}", message = "codigo deve ter de 2 a 20 letras, números ou hífen (ex.: UTI-05)")
        String codigo,
        @NotNull(message = "setor é obrigatório (UTI, ENFERMARIA ou EMERGENCIA)")
        Setor setor
) {
}
