package com.healthsys.monitoring.sinais;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReceptorMqttTest {

    @Test
    void codigoDoLeitoVemDoTopico() {
        assertThat(ReceptorMqtt.codigoDoLeito("healthsys/leitos/UTI-01/sinais")).isEqualTo("UTI-01");
        assertThat(ReceptorMqtt.codigoDoLeito("healthsys/leitos/enf-02/sinais")).isEqualTo("ENF-02");
    }

    @Test
    void topicoForaDoFormatoEIgnorado() {
        assertThat(ReceptorMqtt.codigoDoLeito("healthsys/leitos//sinais")).isNull();
        assertThat(ReceptorMqtt.codigoDoLeito("outro/leitos/UTI-01/sinais")).isNull();
        assertThat(ReceptorMqtt.codigoDoLeito("healthsys/leitos/UTI-01")).isNull();
        assertThat(ReceptorMqtt.codigoDoLeito(null)).isNull();
    }
}
