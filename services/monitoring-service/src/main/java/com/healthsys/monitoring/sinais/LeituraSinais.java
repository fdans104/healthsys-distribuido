package com.healthsys.monitoring.sinais;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Uma medição enviada pelo monitor do leito (via MQTT), em JSON:
 * {"frequenciaCardiaca":82,"saturacao":97,"temperatura":36.8,"pressaoSistolica":120,"pressaoDiastolica":80,
 *  "medidoEm":"2026-11-22T10:00:00Z"}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeituraSinais(
        Integer frequenciaCardiaca,
        Integer saturacao,
        BigDecimal temperatura,
        Integer pressaoSistolica,
        Integer pressaoDiastolica,
        String medidoEm
) {
    public boolean vazia() {
        return frequenciaCardiaca == null && saturacao == null && temperatura == null
                && pressaoSistolica == null && pressaoDiastolica == null;
    }
}
