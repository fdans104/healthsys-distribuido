package com.healthsys.triage.triagem.dto;

import com.healthsys.triage.triagem.StatusTriagem;
import jakarta.validation.constraints.NotNull;

/** Corpo do PUT /api/v1/triagens/{id}/status (RF16). */
public record StatusRequest(
        @NotNull(message = "status é obrigatório (PENDENTE, EM_ATENDIMENTO, FINALIZADA ou CANCELADA)")
        StatusTriagem status,

        @NotNull(message = "versao é obrigatória")
        Long versao
) {
}
