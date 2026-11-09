package com.healthsys.patient.exception;

/** RN54: o usuário tentou salvar um paciente com base em uma versão que já foi substituída por outra alteração. */
public class ConflitoEdicaoException extends RuntimeException {

    public static final String MENSAGEM =
            "Este paciente foi atualizado por outro usuário. Carregue a versão mais recente antes de fazer uma nova alteração.";

    public ConflitoEdicaoException() {
        super(MENSAGEM);
    }
}