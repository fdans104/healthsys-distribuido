package com.healthsys.triage.exception;

/** Pedido válido no formato, mas que contraria uma regra do prontuário (ex.: finalizar uma triagem sem classificação de risco). */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
