package com.healthsys.monitoring.sinais.dto;

import java.util.List;

/** O que a tela de monitoramento recebe: se a conexão com os monitores (MQTT) está ok e os leitos ocupados. */
public record PainelMonitoramentoResponse(boolean monitoresConectados, List<MonitorLeitoResponse> leitos) {
}
