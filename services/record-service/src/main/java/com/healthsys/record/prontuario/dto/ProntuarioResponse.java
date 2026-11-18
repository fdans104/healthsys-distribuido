package com.healthsys.record.prontuario.dto;

import com.healthsys.record.prontuario.Consulta;
import com.healthsys.record.prontuario.Exame;
import com.healthsys.record.prontuario.Medicamento;

import java.util.List;

/** RF10: o prontuário do paciente, com consultas, exames e medicamentos, cada lista do mais recente para o mais antigo. */
public record ProntuarioResponse(
        Integer pacienteId,
        List<Consulta> consultas,
        List<Exame> exames,
        List<Medicamento> medicamentos
) {
}
