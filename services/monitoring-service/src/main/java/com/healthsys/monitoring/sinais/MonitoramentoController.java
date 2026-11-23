package com.healthsys.monitoring.sinais;

import com.healthsys.monitoring.leito.Profissional;
import com.healthsys.monitoring.sinais.dto.AlertaResponse;
import com.healthsys.monitoring.sinais.dto.PainelMonitoramentoResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Sinais vitais ao vivo e alertas (só MEDICO e ENFERMEIRO, ver SecurityConfig). */
@RestController
@RequestMapping("/api/v1/monitoramento")
public class MonitoramentoController {

    private final MonitoramentoService service;
    private final ReceptorMqtt receptor;

    public MonitoramentoController(MonitoramentoService service, ReceptorMqtt receptor) {
        this.service = service;
        this.receptor = receptor;
    }

    @GetMapping("/leitos")
    public PainelMonitoramentoResponse leitos() {
        return new PainelMonitoramentoResponse(receptor.conectado(), service.leitosMonitorados());
    }

    /** GET /api/v1/monitoramento/alertas?ativos=true (padrão) ou ?ativos=false (últimos 100). */
    @GetMapping("/alertas")
    public List<AlertaResponse> alertas(@RequestParam(defaultValue = "true") boolean ativos) {
        return service.alertas(ativos);
    }

    @PutMapping("/alertas/{id}/reconhecer")
    public AlertaResponse reconhecer(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        return service.reconhecer(id, Profissional.doToken(jwt));
    }
}
