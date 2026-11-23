package com.healthsys.triage.triagem;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Métrica healthsys_triagens_fila{risco=...}: quantos pacientes estão na fila (aguardando ou em atendimento)
 * por classificação de risco. Aparece no painel do Grafana.
 */
@Component
public class MetricasDaFila {

    private static final Set<StatusTriagem> NA_FILA = EnumSet.of(StatusTriagem.PENDENTE, StatusTriagem.EM_ATENDIMENTO);

    public MetricasDaFila(TriagemRepository repository, MeterRegistry metricas) {
        for (ClassificacaoRisco risco : ClassificacaoRisco.values()) {
            Gauge.builder("healthsys.triagens.fila", () -> repository.countByStatusInAndClassificacaoRisco(NA_FILA, risco))
                    .description("Pacientes na fila da triagem por classificação de risco")
                    .tag("risco", risco.name())
                    .register(metricas);
        }
        Gauge.builder("healthsys.triagens.fila", () -> repository.countByStatusInAndClassificacaoRiscoIsNull(NA_FILA))
                .description("Pacientes na fila da triagem por classificação de risco")
                .tag("risco", "SEM_CLASSIFICACAO")
                .register(metricas);
    }
}
