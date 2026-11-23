package com.healthsys.monitoring.leito;

import com.healthsys.monitoring.leito.dto.LeitoRequest;
import com.healthsys.monitoring.leito.dto.LeitoResponse;
import com.healthsys.monitoring.leito.dto.OcupacaoRequest;
import com.healthsys.monitoring.leito.dto.OcupacaoSetorResponse;
import com.healthsys.monitoring.leito.dto.VersaoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/** Gestão de leitos (quem pode o quê está em SecurityConfig). */
@RestController
@RequestMapping("/api/v1/leitos")
public class LeitoController {

    private final LeitoService service;

    public LeitoController(LeitoService service) {
        this.service = service;
    }

    /** O administrador vê a situação dos leitos, mas não quem está internado (dado de saúde). */
    @GetMapping
    public List<LeitoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        List<LeitoResponse> leitos = service.listar();
        if ("ADMIN".equals(jwt.getClaimAsString("perfil"))) {
            return leitos.stream().map(LeitoResponse::semPaciente).toList();
        }
        return leitos;
    }

    @GetMapping("/ocupacao")
    public List<OcupacaoSetorResponse> ocupacao() {
        return service.ocupacao();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeitoResponse criar(@Valid @RequestBody LeitoRequest req, @AuthenticationPrincipal Jwt jwt) {
        return service.criar(req, Profissional.doToken(jwt)).semPaciente();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Integer id) {
        service.remover(id);
    }

    @PutMapping("/{id}/ocupar")
    public LeitoResponse ocupar(@PathVariable Integer id, @Valid @RequestBody OcupacaoRequest req,
                                @AuthenticationPrincipal Jwt jwt,
                                @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return service.ocupar(id, req, Profissional.doToken(jwt), authorization);
    }

    @PutMapping("/{id}/liberar")
    public LeitoResponse liberar(@PathVariable Integer id, @Valid @RequestBody VersaoRequest req,
                                 @AuthenticationPrincipal Jwt jwt) {
        return service.liberar(id, req.versao(), Profissional.doToken(jwt));
    }

    @PutMapping("/{id}/higienizado")
    public LeitoResponse higienizado(@PathVariable Integer id, @Valid @RequestBody VersaoRequest req,
                                     @AuthenticationPrincipal Jwt jwt) {
        return service.marcarHigienizado(id, req.versao(), Profissional.doToken(jwt));
    }
}
