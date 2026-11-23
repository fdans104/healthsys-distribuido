package com.healthsys.monitoring.sinais;

/** Cada tipo de alerta e o sinal vital a que se refere (usado para pintar o valor na tela). */
public enum TipoAlerta {
    SATURACAO_BAIXA("saturacao"),
    TAQUICARDIA("frequenciaCardiaca"),
    BRADICARDIA("frequenciaCardiaca"),
    FEBRE("temperatura"),
    HIPOTERMIA("temperatura"),
    HIPERTENSAO("pressao"),
    HIPOTENSAO("pressao");

    private final String sinal;

    TipoAlerta(String sinal) {
        this.sinal = sinal;
    }

    public String sinal() {
        return sinal;
    }
}
