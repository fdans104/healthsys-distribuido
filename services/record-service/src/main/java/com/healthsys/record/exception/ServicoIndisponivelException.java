package com.healthsys.record.exception;

/** Outro microsserviço necessário para a operação (ex.: o de pacientes) não respondeu. */
public class ServicoIndisponivelException extends RuntimeException {
    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
