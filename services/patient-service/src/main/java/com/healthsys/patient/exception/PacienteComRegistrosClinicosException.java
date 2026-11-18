package com.healthsys.patient.exception;

/**
 * O paciente já tem registros clínicos (atendimentos, vacinas ou alergias).
 * Prontuário não pode ser apagado, então o cadastro também não pode ser excluído.
 */
public class PacienteComRegistrosClinicosException extends RuntimeException {
    public PacienteComRegistrosClinicosException() {
        super("Este paciente já possui registros clínicos (prontuário) e por isso não pode ser excluído.");
    }
}
