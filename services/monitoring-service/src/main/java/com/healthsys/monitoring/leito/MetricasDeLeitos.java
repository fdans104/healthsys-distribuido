package com.healthsys.monitoring.leito;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Métrica healthsys_leitos{setor, status}: ocupação hospitalar no Grafana. */
@Component
public class MetricasDeLeitos {

    public MetricasDeLeitos(LeitoRepository leitos, MeterRegistry metricas) {
        for (Setor setor : Setor.values()) {
            for (StatusLeito status : StatusLeito.values()) {
                Gauge.builder("healthsys.leitos", () -> leitos.countBySetorAndStatus(setor, status))
                        .description("Leitos por setor e situação")
                        .tag("setor", setor.name())
                        .tag("status", status.name())
                        .register(metricas);
            }
        }
    }
}
