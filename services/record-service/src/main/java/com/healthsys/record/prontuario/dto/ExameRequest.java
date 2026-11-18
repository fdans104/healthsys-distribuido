package com.healthsys.record.prontuario.dto;

import com.healthsys.record.prontuario.TipoExame;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo do POST /api/v1/prontuarios/{pacienteId}/exames (o médico solicita o exame). */
public record ExameRequest(
        @NotBlank(message = "nome do exame é obrigatório (ex.: Hemograma completo)")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String nome,

        @NotNull(message = "tipo é obrigatório (LABORATORIAL, IMAGEM ou OUTRO)")
        TipoExame tipo,

        @Size(max = 1000, message = "observacoes deve ter no máximo 1000 caracteres")
        String observacoes
) {
}
