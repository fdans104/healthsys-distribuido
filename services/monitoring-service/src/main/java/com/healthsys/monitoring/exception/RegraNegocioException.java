package com.healthsys.monitoring.exception;

/** Pedido válido no formato, mas que contraria uma regra (ex.: internar um paciente em um leito ocupado). */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
