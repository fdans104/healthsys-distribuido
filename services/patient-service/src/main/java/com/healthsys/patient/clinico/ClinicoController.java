package com.healthsys.patient.clinico;

import com.healthsys.patient.clinico.dto.AlergiaRequest;
import com.healthsys.patient.clinico.dto.AlergiaResponse;
import com.healthsys.patient.clinico.dto.AtendimentoRequest;
import com.healthsys.patient.clinico.dto.AtendimentoResponse;
import com.healthsys.patient.clinico.dto.HistoricoItem;
import com.healthsys.patient.clinico.dto.VacinaRequest;
import com.healthsys.patient.clinico.dto.VacinaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints da ficha clínica do paciente. Acesso restrito a MEDICO e ENFERMEIRO (dado clínico é sigiloso);
 * o que cada um pode registrar está em SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/pacientes/{pacienteId}")
public class ClinicoController {

    private final ClinicoService service;

    public ClinicoController(ClinicoService service) {
        this.service = service;
    }

    /** RF07: histórico clínico em ordem de data (mais recente primeiro). */
    @GetMapping("/historico")
    public List<HistoricoItem> historico(@PathVariable Integer pacienteId) {
        return service.historico(pacienteId);
    }

    @GetMapping("/alergias")
    public List<AlergiaResponse> listarAlergias(@PathVariable Integer pacienteId) {
        return service.listarAlergias(pacienteId);
    }

    @PostMapping("/alergias")
    @ResponseStatus(HttpStatus.CREATED)
    public AlergiaResponse registrarAlergia(@PathVariable Integer pacienteId,
                                            @Valid @RequestBody AlergiaRequest req,
                                            @AuthenticationPrincipal Jwt jwt) {
        return service.registrarAlergia(pacienteId, req, Profissional.doToken(jwt));
    }

    @GetMapping("/vacinas")
    public List<VacinaResponse> listarVacinas(@PathVariable Integer pacienteId) {
        return service.listarVacinas(pacienteId);
    }

    @PostMapping("/vacinas")
    @ResponseStatus(HttpStatus.CREATED)
    public VacinaResponse registrarVacina(@PathVariable Integer pacienteId,
                                          @Valid @RequestBody VacinaRequest req,
                                          @AuthenticationPrincipal Jwt jwt) {
        return service.registrarVacina(pacienteId, req, Profissional.doToken(jwt));
    }

    @GetMapping("/atendimentos")
    public List<AtendimentoResponse> listarAtendimentos(@PathVariable Integer pacienteId) {
        return service.listarAtendimentos(pacienteId);
    }

    @PostMapping("/atendimentos")
    @ResponseStatus(HttpStatus.CREATED)
    public AtendimentoResponse registrarAtendimento(@PathVariable Integer pacienteId,
                                                    @Valid @RequestBody AtendimentoRequest req,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return service.registrarAtendimento(pacienteId, req, Profissional.doToken(jwt));
    }
}
