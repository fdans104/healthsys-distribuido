package com.healthsys.triage.triagem;

import com.healthsys.triage.triagem.dto.AberturaRequest;
import com.healthsys.triage.triagem.dto.AvaliacaoRequest;
import com.healthsys.triage.triagem.dto.StatusRequest;
import com.healthsys.triage.triagem.dto.TriagemResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints da teletriagem (quem pode o quê está em SecurityConfig e no TriagemService). */
@RestController
@RequestMapping("/api/v1/triagens")
public class TriagemController {

    private final TriagemService service;

    public TriagemController(TriagemService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TriagemResponse abrir(@Valid @RequestBody AberturaRequest req, @AuthenticationPrincipal Jwt jwt,
                                 @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return paraQuemPediu(service.abrir(req, Profissional.doToken(jwt), authorization), jwt);
    }

    /** GET /api/v1/triagens?status=PENDENTE  ou  ?pacienteId=1 */
    @GetMapping
    public List<TriagemResponse> listar(@RequestParam(required = false) StatusTriagem status,
                                        @RequestParam(required = false) Integer pacienteId,
                                        @AuthenticationPrincipal Jwt jwt) {
        return service.listar(status, pacienteId).stream().map(t -> paraQuemPediu(t, jwt)).toList();
    }

    @GetMapping("/{id}")
    public TriagemResponse buscar(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        return paraQuemPediu(service.buscar(id), jwt);
    }

    @PutMapping("/{id}/avaliacao")
    public TriagemResponse avaliar(@PathVariable Integer id, @Valid @RequestBody AvaliacaoRequest req,
                                   @AuthenticationPrincipal Jwt jwt) {
        return service.avaliar(id, req, Profissional.doToken(jwt));
    }

    @PutMapping("/{id}/status")
    public TriagemResponse mudarStatus(@PathVariable Integer id, @Valid @RequestBody StatusRequest req,
                                       @AuthenticationPrincipal Jwt jwt) {
        return paraQuemPediu(service.mudarStatus(id, req, Profissional.doToken(jwt)), jwt);
    }

    /** A recepção recebe a triagem sem sintomas e sinais vitais. */
    private static TriagemResponse paraQuemPediu(TriagemResponse triagem, Jwt jwt) {
        return "RECEPCIONISTA".equals(jwt.getClaimAsString("perfil")) ? triagem.semDadosClinicos() : triagem;
    }
}
