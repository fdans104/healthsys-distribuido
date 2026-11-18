package com.healthsys.patient.clinico.dto;

import java.time.LocalDateTime;

/**
 * Uma linha do histórico clínico (RF07): atendimentos, vacinas e alergias juntos, em ordem de data.
 * categoria: ATENDIMENTO, VACINA ou ALERGIA.
 */
public record HistoricoItem(
        String categoria,
        Integer id,
        LocalDateTime data,
        String titulo,
        String detalhe,
        String registradoPorNome,
        String registradoPorPerfil
) {
}
