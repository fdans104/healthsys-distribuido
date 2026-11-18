package com.healthsys.patient.clinico.dto;

import com.healthsys.patient.clinico.TipoAtendimento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Corpo do POST /api/v1/pacientes/{id}/atendimentos (RN16: tipo e data são obrigatórios). */
public record AtendimentoRequest(
        @NotNull(message = "tipo é obrigatório (CONSULTA, RETORNO, URGENCIA ou EMERGENCIA)")
        TipoAtendimento tipo,

        @NotNull(message = "data é obrigatória (formato AAAA-MM-DDTHH:MM)")
        @PastOrPresent(message = "data não pode estar no futuro: registre o atendimento depois que ele acontecer")
        LocalDateTime data,

        @Size(max = 4000, message = "observacoes deve ter no máximo 4000 caracteres")
        String observacoes
) {
}
