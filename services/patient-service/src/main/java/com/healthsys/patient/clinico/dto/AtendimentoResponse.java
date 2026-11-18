package com.healthsys.patient.clinico.dto;

import com.healthsys.patient.clinico.Atendimento;
import com.healthsys.patient.clinico.TipoAtendimento;

import java.time.LocalDateTime;

public record AtendimentoResponse(
        Integer id,
        Integer pacienteId,
        TipoAtendimento tipo,
        LocalDateTime data,
        String observacoes,
        LocalDateTime registradoEm,
        String registradoPorNome,
        String registradoPorPerfil
) {
    public static AtendimentoResponse de(Atendimento a) {
        return new AtendimentoResponse(a.getId(), a.getPacienteId(), a.getTipo(), a.getData(), a.getObservacoes(),
                a.getRegistradoEm(), a.getRegistradoPorNome(), a.getRegistradoPorPerfil());
    }
}
