package com.healthsys.patient.clinico.dto;

import com.healthsys.patient.clinico.Vacina;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VacinaResponse(
        Integer id,
        Integer pacienteId,
        String nomeVacina,
        LocalDate dataAplicacao,
        String dose,
        String lote,
        LocalDateTime registradoEm,
        String registradoPorNome,
        String registradoPorPerfil
) {
    public static VacinaResponse de(Vacina v) {
        return new VacinaResponse(v.getId(), v.getPacienteId(), v.getNomeVacina(), v.getDataAplicacao(),
                v.getDose(), v.getLote(), v.getRegistradoEm(), v.getRegistradoPorNome(), v.getRegistradoPorPerfil());
    }
}
