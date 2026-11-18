package com.healthsys.triage.paciente;

/** O pouco que a triagem precisa saber do paciente, lido do patient-service. */
public record PacienteResumo(Integer id, String nome) {
}
