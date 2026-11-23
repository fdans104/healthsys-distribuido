package com.healthsys.monitoring.sinais;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Confere cada leitura contra faixas de referência e diz o que está fora do normal.
 * ATENÇÃO: os limites abaixo são simplificados, só para demonstração acadêmica.
 * Num hospital de verdade, eles seguem o protocolo clínico da instituição (ex.: escore de alerta precoce)
 * e podem ser diferentes para cada paciente.
 *
 * Sinal                 Atenção                      Crítico
 * Saturação (SpO2)      abaixo de 94%                abaixo de 90%
 * Frequência cardíaca   acima de 110 ou abaixo de 50 acima de 130 ou abaixo de 40 bpm
 * Temperatura           38,0 °C ou mais / abaixo 35  39,5 °C ou mais / abaixo de 34 °C
 * Pressão sistólica     160 ou mais / abaixo de 90   180 ou mais / abaixo de 80 mmHg
 */
public final class AvaliadorSinaisVitais {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final BigDecimal FEBRE = new BigDecimal("38.0");
    private static final BigDecimal FEBRE_ALTA = new BigDecimal("39.5");
    private static final BigDecimal HIPOTERMIA = new BigDecimal("35.0");
    private static final BigDecimal HIPOTERMIA_GRAVE = new BigDecimal("34.0");

    private AvaliadorSinaisVitais() {
    }

    public static List<Achado> avaliar(LeituraSinais l) {
        List<Achado> achados = new ArrayList<>();
        if (l == null) {
            return achados;
        }

        Integer spo2 = l.saturacao();
        if (spo2 != null) {
            if (spo2 < 90) {
                achados.add(new Achado(TipoAlerta.SATURACAO_BAIXA, GravidadeAlerta.CRITICO, spo2 + "%",
                        "saturação de oxigênio em " + spo2 + "% (crítica)"));
            } else if (spo2 < 94) {
                achados.add(new Achado(TipoAlerta.SATURACAO_BAIXA, GravidadeAlerta.ATENCAO, spo2 + "%",
                        "saturação de oxigênio em " + spo2 + "% (baixa)"));
            }
        }

        Integer fc = l.frequenciaCardiaca();
        if (fc != null) {
            if (fc > 130) {
                achados.add(new Achado(TipoAlerta.TAQUICARDIA, GravidadeAlerta.CRITICO, fc + " bpm",
                        "frequência cardíaca em " + fc + " bpm (taquicardia importante)"));
            } else if (fc > 110) {
                achados.add(new Achado(TipoAlerta.TAQUICARDIA, GravidadeAlerta.ATENCAO, fc + " bpm",
                        "frequência cardíaca em " + fc + " bpm (taquicardia)"));
            } else if (fc < 40) {
                achados.add(new Achado(TipoAlerta.BRADICARDIA, GravidadeAlerta.CRITICO, fc + " bpm",
                        "frequência cardíaca em " + fc + " bpm (bradicardia importante)"));
            } else if (fc < 50) {
                achados.add(new Achado(TipoAlerta.BRADICARDIA, GravidadeAlerta.ATENCAO, fc + " bpm",
                        "frequência cardíaca em " + fc + " bpm (bradicardia)"));
            }
        }

        BigDecimal temp = l.temperatura();
        if (temp != null) {
            String t = String.format(PT_BR, "%.1f °C", temp);
            if (temp.compareTo(FEBRE_ALTA) >= 0) {
                achados.add(new Achado(TipoAlerta.FEBRE, GravidadeAlerta.CRITICO, t, "temperatura em " + t + " (febre alta)"));
            } else if (temp.compareTo(FEBRE) >= 0) {
                achados.add(new Achado(TipoAlerta.FEBRE, GravidadeAlerta.ATENCAO, t, "temperatura em " + t + " (febre)"));
            } else if (temp.compareTo(HIPOTERMIA_GRAVE) < 0) {
                achados.add(new Achado(TipoAlerta.HIPOTERMIA, GravidadeAlerta.CRITICO, t, "temperatura em " + t + " (hipotermia grave)"));
            } else if (temp.compareTo(HIPOTERMIA) < 0) {
                achados.add(new Achado(TipoAlerta.HIPOTERMIA, GravidadeAlerta.ATENCAO, t, "temperatura em " + t + " (hipotermia)"));
            }
        }

        Integer pas = l.pressaoSistolica();
        if (pas != null) {
            String pa = pas + (l.pressaoDiastolica() == null ? "" : "/" + l.pressaoDiastolica()) + " mmHg";
            if (pas >= 180) {
                achados.add(new Achado(TipoAlerta.HIPERTENSAO, GravidadeAlerta.CRITICO, pa, "pressão arterial em " + pa + " (muito alta)"));
            } else if (pas >= 160) {
                achados.add(new Achado(TipoAlerta.HIPERTENSAO, GravidadeAlerta.ATENCAO, pa, "pressão arterial em " + pa + " (alta)"));
            } else if (pas < 80) {
                achados.add(new Achado(TipoAlerta.HIPOTENSAO, GravidadeAlerta.CRITICO, pa, "pressão arterial em " + pa + " (muito baixa)"));
            } else if (pas < 90) {
                achados.add(new Achado(TipoAlerta.HIPOTENSAO, GravidadeAlerta.ATENCAO, pa, "pressão arterial em " + pa + " (baixa)"));
            }
        }
        return achados;
    }
}
