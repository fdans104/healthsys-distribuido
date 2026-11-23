package com.healthsys.monitoring.sinais;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RegraDeRepeticaoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 22, 10, 0);

    private static Alerta alerta(Integer paciente, GravidadeAlerta gravidade) {
        return new Alerta("UTI-01", "UTI", paciente, "Maria",
                new Achado(TipoAlerta.SATURACAO_BAIXA, gravidade, "88%", "saturação baixa"), AGORA.minusMinutes(1));
    }

    @Test
    void primeiroAlertaSempreAvisa() {
        assertThat(RegraDeRepeticao.deveAlertar(null, 1, GravidadeAlerta.ATENCAO, AGORA)).isTrue();
    }

    @Test
    void alertaAbertoNaoSeRepeteACadaLeitura() {
        assertThat(RegraDeRepeticao.deveAlertar(alerta(1, GravidadeAlerta.ATENCAO), 1, GravidadeAlerta.ATENCAO, AGORA)).isFalse();
        assertThat(RegraDeRepeticao.deveAlertar(alerta(1, GravidadeAlerta.CRITICO), 1, GravidadeAlerta.CRITICO, AGORA)).isFalse();
        assertThat(RegraDeRepeticao.deveAlertar(alerta(1, GravidadeAlerta.CRITICO), 1, GravidadeAlerta.ATENCAO, AGORA)).isFalse();
    }

    @Test
    void sePiorarDeAtencaoParaCriticoAvisaDeNovo() {
        assertThat(RegraDeRepeticao.deveAlertar(alerta(1, GravidadeAlerta.ATENCAO), 1, GravidadeAlerta.CRITICO, AGORA)).isTrue();
    }

    @Test
    void depoisDeReconhecidoEsperaCincoMinutosAntesDeRepetir() {
        Alerta visto = alerta(1, GravidadeAlerta.ATENCAO);
        visto.reconhecer("Enf. Bruno", AGORA.minusMinutes(2));
        assertThat(RegraDeRepeticao.deveAlertar(visto, 1, GravidadeAlerta.ATENCAO, AGORA)).isFalse();

        Alerta vistoHaMuitoTempo = alerta(1, GravidadeAlerta.ATENCAO);
        vistoHaMuitoTempo.reconhecer("Enf. Bruno", AGORA.minusMinutes(6));
        assertThat(RegraDeRepeticao.deveAlertar(vistoHaMuitoTempo, 1, GravidadeAlerta.ATENCAO, AGORA)).isTrue();
    }

    @Test
    void outroPacienteNoLeitoNaoHerdaOSilencio() {
        assertThat(RegraDeRepeticao.deveAlertar(alerta(1, GravidadeAlerta.ATENCAO), 2, GravidadeAlerta.ATENCAO, AGORA)).isTrue();
    }
}
