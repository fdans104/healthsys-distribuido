package com.healthsys.record.prontuario.dto;

import com.healthsys.record.prontuario.ViaAdministracao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Corpo do POST /api/v1/prontuarios/{pacienteId}/medicamentos (o médico prescreve). */
public record MedicamentoRequest(
        @NotBlank(message = "nome do medicamento é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "dosagem é obrigatória (ex.: 500 mg)")
        @Size(max = 100, message = "dosagem deve ter no máximo 100 caracteres")
        String dosagem,

        @NotNull(message = "via é obrigatória (ORAL, INTRAVENOSA, INTRAMUSCULAR, SUBCUTANEA, TOPICA, INALATORIA ou OUTRA)")
        ViaAdministracao via,

        @NotBlank(message = "frequencia é obrigatória (ex.: de 8 em 8 horas)")
        @Size(max = 100, message = "frequencia deve ter no máximo 100 caracteres")
        String frequencia,

        @NotNull(message = "dataInicio é obrigatória (formato AAAA-MM-DD)")
        LocalDate dataInicio,

        LocalDate dataFim,

        @Size(max = 1000, message = "observacoes deve ter no máximo 1000 caracteres")
        String observacoes
) {
}
