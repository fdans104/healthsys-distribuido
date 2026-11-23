package com.healthsys.notification.tempo_real;

import com.healthsys.notification.notificacao.Notificacao;
import com.healthsys.notification.notificacao.NotificacaoResponse;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notificações em tempo real com Server-Sent Events (SSE): cada tela logada mantém uma conexão aberta
 * e recebe o aviso no mesmo instante em que o evento chega do Kafka, sem precisar ficar perguntando.
 * Se a conexão cair, a tela reconecta sozinha e, enquanto isso, consulta a lista de tempos em tempos.
 */
@Component
public class CanalTempoReal {

    private static final long DURACAO_MAXIMA_MS = Duration.ofMinutes(30).toMillis();

    private final Map<String, Assinante> assinantes = new ConcurrentHashMap<>();

    public CanalTempoReal(MeterRegistry metricas) {
        Gauge.builder("healthsys.notificacoes.conexoes", assinantes, Map::size)
                .description("Telas conectadas recebendo notificações em tempo real")
                .register(metricas);
    }

    public SseEmitter assinar(Integer usuarioId, String perfil) {
        SseEmitter emissor = new SseEmitter(DURACAO_MAXIMA_MS);
        String chave = UUID.randomUUID().toString();
        assinantes.put(chave, new Assinante(usuarioId, perfil, emissor));
        emissor.onCompletion(() -> assinantes.remove(chave));
        emissor.onTimeout(() -> {
            assinantes.remove(chave);
            emissor.complete();
        });
        emissor.onError(erro -> assinantes.remove(chave));
        enviar(chave, emissor, SseEmitter.event().name("conectado").data("ok"));
        return emissor;
    }

    /** Entrega a notificação a todas as telas abertas de quem deve recebê-la. */
    public void publicar(Notificacao notificacao) {
        assinantes.forEach((chave, a) -> {
            if (notificacao.destinadaA(a.usuarioId(), a.perfil())) {
                enviar(chave, a.emissor(), SseEmitter.event()
                        .name("notificacao")
                        .data(NotificacaoResponse.de(notificacao, a.usuarioId()), MediaType.APPLICATION_JSON));
            }
        });
    }

    /** A cada 20 s manda um "sinal de vida", para proxies não fecharem a conexão parada. */
    @Scheduled(fixedRate = 20000)
    public void manterConexoesVivas() {
        assinantes.forEach((chave, a) -> enviar(chave, a.emissor(), SseEmitter.event().comment("ping")));
    }

    int conexoesAbertas() {
        return assinantes.size();
    }

    private void enviar(String chave, SseEmitter emissor, SseEmitter.SseEventBuilder evento) {
        try {
            emissor.send(evento);
        } catch (IOException | IllegalStateException e) {
            // A tela foi fechada ou a conexão caiu: esquece essa conexão.
            assinantes.remove(chave);
        }
    }

    private record Assinante(Integer usuarioId, String perfil, SseEmitter emissor) {
    }
}
