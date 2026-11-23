package com.healthsys.monitoring.leito.dto;

import jakarta.validation.constraints.NotNull;

/** Corpo do PUT .../liberar e .../higienizado: a versão do leito que a tela mostrava (RN54). */
public record VersaoRequest(
        @NotNull(message = "versao é obrigatória")
        Long versao
) {
}
