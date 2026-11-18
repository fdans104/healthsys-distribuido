package com.healthsys.triage.triagem;

/** RN40: só estes quatro status. FINALIZADA e CANCELADA encerram a triagem. */
public enum StatusTriagem {
    PENDENTE,
    EM_ATENDIMENTO,
    FINALIZADA,
    CANCELADA;

    public boolean encerrada() {
        return this == FINALIZADA || this == CANCELADA;
    }
}
