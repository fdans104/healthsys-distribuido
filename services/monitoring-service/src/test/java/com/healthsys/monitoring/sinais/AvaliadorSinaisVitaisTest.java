package com.healthsys.monitoring.sinais;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvaliadorSinaisVitaisTest {

    private static LeituraSinais leitura(Integer fc, Integer spo2, String temp, Integer pas, Integer pad) {
        return new LeituraSinais(fc, spo2, temp == null ? null : new BigDecimal(temp), pas, pad, null);
    }

    @Test
    void sinaisNormaisNaoGeramAlerta() {
        assertThat(AvaliadorSinaisVitais.avaliar(leitura(80, 97, "36.8", 120, 80))).isEmpty();
    }

    @Test
    void saturacaoAbaixoDe90ECritica() {
        List<Achado> achados = AvaliadorSinaisVitais.avaliar(leitura(80, 86, "36.8", 120, 80));

        assertThat(achados).hasSize(1);
        assertThat(achados.get(0).tipo()).isEqualTo(TipoAlerta.SATURACAO_BAIXA);
        assertThat(achados.get(0).gravidade()).isEqualTo(GravidadeAlerta.CRITICO);
        assertThat(achados.get(0).mensagem()).isEqualTo("saturação de oxigênio em 86% (crítica)");
    }

    @Test
    void saturacaoEntre90E93PedeAtencao() {
        Achado a = AvaliadorSinaisVitais.avaliar(leitura(80, 92, "36.8", 120, 80)).get(0);
        assertThat(a.gravidade()).isEqualTo(GravidadeAlerta.ATENCAO);
    }

    @Test
    void limitesExatos() {
        // 94% de saturação, 110 bpm, 37,9 °C e 159 mmHg ainda são normais.
        assertThat(AvaliadorSinaisVitais.avaliar(leitura(110, 94, "37.9", 159, 95))).isEmpty();
        // 38,0 °C já é febre; 39,5 °C é febre alta.
        assertThat(AvaliadorSinaisVitais.avaliar(leitura(80, 97, "38.0", 120, 80)).get(0).gravidade())
                .isEqualTo(GravidadeAlerta.ATENCAO);
        assertThat(AvaliadorSinaisVitais.avaliar(leitura(80, 97, "39.5", 120, 80)).get(0).gravidade())
                .isEqualTo(GravidadeAlerta.CRITICO);
    }

    @Test
    void variosSinaisRuinsGeramVariosAchados() {
        List<Achado> achados = AvaliadorSinaisVitais.avaliar(leitura(140, 85, "39.8", 75, 40));

        assertThat(achados).extracting(Achado::tipo).containsExactly(
                TipoAlerta.SATURACAO_BAIXA, TipoAlerta.TAQUICARDIA, TipoAlerta.FEBRE, TipoAlerta.HIPOTENSAO);
        assertThat(achados).allMatch(a -> a.gravidade() == GravidadeAlerta.CRITICO);
    }

    @Test
    void temperaturaUsaVirgulaNaMensagem() {
        Achado a = AvaliadorSinaisVitais.avaliar(leitura(80, 97, "39.6", 120, 80)).get(0);
        assertThat(a.valor()).isEqualTo("39,6 °C");
    }

    @Test
    void bradicardiaEHipotermia() {
        List<Achado> achados = AvaliadorSinaisVitais.avaliar(leitura(38, 97, "33.5", 120, 80));
        assertThat(achados).extracting(Achado::tipo).containsExactly(TipoAlerta.BRADICARDIA, TipoAlerta.HIPOTERMIA);
    }

    @Test
    void sinalAusenteNaoEAvaliado() {
        assertThat(AvaliadorSinaisVitais.avaliar(leitura(null, null, null, null, null))).isEmpty();
        assertThat(AvaliadorSinaisVitais.avaliar(null)).isEmpty();
    }
}
