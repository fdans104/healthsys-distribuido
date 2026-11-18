package com.healthsys.patient.clinico.dto;

import com.healthsys.patient.clinico.Alergia;

import java.time.LocalDateTime;

public record AlergiaResponse(
        Integer id,
        Integer pacienteId,
        String descricao,
        LocalDateTime registradoEm,
        String registradoPorNome,
        String registradoPorPerfil
) {
    public static AlergiaResponse de(Alergia a) {
        return new AlergiaResponse(a.getId(), a.getPacienteId(), a.getDescricao(), a.getRegistradoEm(),
                a.getRegistradoPorNome(), a.getRegistradoPorPerfil());
    }
}
