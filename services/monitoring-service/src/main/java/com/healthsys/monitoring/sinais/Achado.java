package com.healthsys.monitoring.sinais;

/** Um sinal vital fora da faixa esperada, encontrado em uma leitura. */
public record Achado(TipoAlerta tipo, GravidadeAlerta gravidade, String valor, String mensagem) {
}
