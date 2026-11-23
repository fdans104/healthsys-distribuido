package com.healthsys.monitoring.sinais.dto;

import com.healthsys.monitoring.leito.Setor;
import com.healthsys.monitoring.sinais.LeituraSinais;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Um leito ocupado com a última leitura do monitor.
 * estados: para cada sinal ("saturacao", "frequenciaCardiaca", "temperatura", "pressao"), NORMAL, ATENCAO ou CRITICO.
 */
public record MonitorLeitoResponse(
        Integer leitoId,
        String codigo,
        Setor setor,
        Integer pacienteId,
        String pacienteNome,
        LocalDateTime ocupadoEm,
        LeituraSinais leitura,
        Instant recebidaEm,
        boolean semSinal,
        Map<String, String> estados,
        long alertasAtivos
) {
}
