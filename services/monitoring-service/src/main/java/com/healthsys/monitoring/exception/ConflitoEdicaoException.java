package com.healthsys.monitoring.exception;

/** RN54: o leito foi alterado por outra pessoa depois que o usuário abriu a tela. */
public class ConflitoEdicaoException extends RuntimeException {

    public static final String MENSAGEM =
            "Este leito foi atualizado por outro usuário. Carregue a versão mais recente antes de fazer uma nova alteração.";

    public ConflitoEdicaoException() {
        super(MENSAGEM);
    }
}
