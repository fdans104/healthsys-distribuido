package com.healthsys.notification.notificacao;

import com.healthsys.notification.tempo_real.CanalTempoReal;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/** Notificações da pessoa logada (o perfil e o id vêm do token). */
@RestController
@RequestMapping("/api/v1/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;
    private final CanalTempoReal tempoReal;

    public NotificacaoController(NotificacaoService service, CanalTempoReal tempoReal) {
        this.service = service;
        this.tempoReal = tempoReal;
    }

    @GetMapping
    public List<NotificacaoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(Profissional.doToken(jwt));
    }

    @GetMapping("/nao-lidas")
    public Map<String, Long> naoLidas(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("total", service.contarNaoLidas(Profissional.doToken(jwt)));
    }

    @PutMapping("/{id}/lida")
    public ResponseEntity<?> marcarLida(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        if (!service.marcarLida(id, Profissional.doToken(jwt))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Notificação não encontrada"));
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/lidas")
    public Map<String, Long> marcarTodas(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("marcadas", service.marcarTodasLidas(Profissional.doToken(jwt)));
    }

    /** Canal em tempo real (Server-Sent Events). A tela mantém esta conexão aberta. */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter tempoReal(@AuthenticationPrincipal Jwt jwt, HttpServletResponse resposta) {
        // Pede ao nginx que não segure os dados no buffer: cada aviso sai na hora.
        resposta.setHeader("X-Accel-Buffering", "no");
        resposta.setHeader("Cache-Control", "no-cache");
        Profissional quem = Profissional.doToken(jwt);
        return tempoReal.assinar(quem.id(), quem.perfil());
    }
}
