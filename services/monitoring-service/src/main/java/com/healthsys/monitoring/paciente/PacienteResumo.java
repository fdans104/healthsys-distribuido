package com.healthsys.monitoring.paciente;

/** O pouco que o monitoramento precisa saber do paciente, lido do patient-service. */
public record PacienteResumo(Integer id, String nome) {
}
