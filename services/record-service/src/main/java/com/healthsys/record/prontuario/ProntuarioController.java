package com.healthsys.record.prontuario;

import com.healthsys.record.prontuario.dto.ConsultaRequest;
import com.healthsys.record.prontuario.dto.ExameRequest;
import com.healthsys.record.prontuario.dto.MedicamentoRequest;
import com.healthsys.record.prontuario.dto.ProntuarioResponse;
import com.healthsys.record.prontuario.dto.ResultadoExameRequest;
import com.healthsys.record.prontuario.dto.SuspensaoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints do prontuário eletrônico (quem pode o quê está em SecurityConfig). */
@RestController
@RequestMapping("/api/v1/prontuarios/{pacienteId}")
public class ProntuarioController {

    private final ProntuarioService service;

    public ProntuarioController(ProntuarioService service) {
        this.service = service;
    }

    @GetMapping
    public ProntuarioResponse prontuario(@PathVariable Integer pacienteId) {
        return service.prontuario(pacienteId);
    }

    @GetMapping("/consultas")
    public List<Consulta> consultas(@PathVariable Integer pacienteId) {
        return service.listarConsultas(pacienteId);
    }

    @PostMapping("/consultas")
    @ResponseStatus(HttpStatus.CREATED)
    public Consulta registrarConsulta(@PathVariable Integer pacienteId, @Valid @RequestBody ConsultaRequest req,
                                      @AuthenticationPrincipal Jwt jwt,
                                      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.registrarConsulta(pacienteId, req, Profissional.doToken(jwt), authorization);
    }

    @GetMapping("/exames")
    public List<Exame> exames(@PathVariable Integer pacienteId) {
        return service.listarExames(pacienteId);
    }

    @PostMapping("/exames")
    @ResponseStatus(HttpStatus.CREATED)
    public Exame solicitarExame(@PathVariable Integer pacienteId, @Valid @RequestBody ExameRequest req,
                                @AuthenticationPrincipal Jwt jwt,
                                @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.solicitarExame(pacienteId, req, Profissional.doToken(jwt), authorization);
    }

    @PutMapping("/exames/{exameId}/resultado")
    public Exame registrarResultado(@PathVariable Integer pacienteId, @PathVariable String exameId,
                                    @Valid @RequestBody ResultadoExameRequest req, @AuthenticationPrincipal Jwt jwt) {
        return service.registrarResultado(pacienteId, exameId, req, Profissional.doToken(jwt));
    }

    @GetMapping("/medicamentos")
    public List<Medicamento> medicamentos(@PathVariable Integer pacienteId) {
        return service.listarMedicamentos(pacienteId);
    }

    @PostMapping("/medicamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public Medicamento prescrever(@PathVariable Integer pacienteId, @Valid @RequestBody MedicamentoRequest req,
                                  @AuthenticationPrincipal Jwt jwt,
                                  @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.prescrever(pacienteId, req, Profissional.doToken(jwt), authorization);
    }

    @PutMapping("/medicamentos/{medicamentoId}/suspensao")
    public Medicamento suspender(@PathVariable Integer pacienteId, @PathVariable String medicamentoId,
                                 @Valid @RequestBody SuspensaoRequest req, @AuthenticationPrincipal Jwt jwt) {
        return service.suspender(pacienteId, medicamentoId, req, Profissional.doToken(jwt));
    }
}
