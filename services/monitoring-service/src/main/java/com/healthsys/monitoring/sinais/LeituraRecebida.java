package com.healthsys.monitoring.sinais;

import java.time.Instant;

/** A última leitura de um leito e quando ela chegou. */
public record LeituraRecebida(LeituraSinais leitura, Instant recebidaEm) {
}
