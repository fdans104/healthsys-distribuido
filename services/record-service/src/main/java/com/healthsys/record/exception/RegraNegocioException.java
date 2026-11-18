package com.healthsys.record.exception;

/** Pedido válido no formato, mas que contraria uma regra do prontuário (ex.: suspender um medicamento já suspenso). */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
