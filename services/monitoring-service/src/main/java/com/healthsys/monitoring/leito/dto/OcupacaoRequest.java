package com.healthsys.monitoring.leito.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Corpo do PUT /api/v1/leitos/{id}/ocupar: quem vai ser internado e a versão do leito que a tela mostrava (RN54). */
public record OcupacaoRequest(
        @NotNull(message = "pacienteId é obrigatório")
        @Positive(message = "pacienteId inválido")
        Integer pacienteId,
        @NotNull(message = "versao é obrigatória")
        Long versao
) {
}
