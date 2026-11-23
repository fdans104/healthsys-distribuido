package com.healthsys.monitoring.exception;

/** O perfil do usuário não pode fazer esta mudança de status (ex.: recepção finalizando um atendimento). */
public class AcaoNaoPermitidaException extends RuntimeException {
    public AcaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
