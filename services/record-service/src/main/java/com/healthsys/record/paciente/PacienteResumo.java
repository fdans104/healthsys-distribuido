package com.healthsys.record.paciente;

/** O pouco que o prontuário precisa saber do paciente, lido do patient-service. */
public record PacienteResumo(Integer id, String nome) {
}
