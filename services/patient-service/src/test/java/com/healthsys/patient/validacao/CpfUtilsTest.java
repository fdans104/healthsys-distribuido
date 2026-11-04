package com.healthsys.patient.validacao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CpfUtilsTest {

    @Test
    void aceitaCpfsValidosComOuSemPontuacao() {
        assertThat(CpfUtils.valido("52998224725")).isTrue();
        assertThat(CpfUtils.valido("529.982.247-25")).isTrue();
        assertThat(CpfUtils.valido("111.444.777-35")).isTrue();
    }

    @Test
    void rejeitaDigitoVerificadorErradoTamanhoErradoERepetidos() {
        assertThat(CpfUtils.valido("52998224724")).isFalse();
        assertThat(CpfUtils.valido("5299822472")).isFalse();
        assertThat(CpfUtils.valido("111.111.111-11")).isFalse();
        assertThat(CpfUtils.valido(null)).isFalse();
    }

    @Test
    void normalizaParaSomenteDigitos() {
        assertThat(CpfUtils.somenteDigitos("529.982.247-25")).isEqualTo("52998224725");
    }
}
