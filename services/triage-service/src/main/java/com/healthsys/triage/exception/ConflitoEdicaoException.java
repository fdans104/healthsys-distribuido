package com.healthsys.triage.exception;

/** RN54: a triagem foi alterada por outra pessoa depois que o usuário a abriu. */
public class ConflitoEdicaoException extends RuntimeException {

    public static final String MENSAGEM =
            "Esta triagem foi atualizada por outro usuário. Carregue a versão mais recente antes de fazer uma nova alteração.";

    public ConflitoEdicaoException() {
        super(MENSAGEM);
    }
}
