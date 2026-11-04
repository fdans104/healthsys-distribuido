package com.healthsys.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Converte "microsserviço fora do ar" em uma resposta 503 padronizada (RFC 7807).
 *
 * Quando um serviço está parado, o gateway recebe um erro de rede (host não resolvido, conexão recusada,
 * conexão interrompida), um timeout, ou o circuit breaker já está aberto e recusa a chamada.
 * Em todos esses casos o frontend recebe 503 com uma mensagem clara, em vez de um 500 genérico.
 *
 * Qualquer outro erro não é tratado aqui e segue para o tratador padrão do Spring.
 * A ordem -2 faz este tratador rodar antes do padrão (que tem ordem -1).
 */
@Component
@Order(-2)
public class ServicoIndisponivelHandler implements WebExceptionHandler {

    private static final int PROFUNDIDADE_MAXIMA = 10;

    private final ObjectMapper mapper;

    public ServicoIndisponivelHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse resposta = exchange.getResponse();
        if (resposta.isCommitted() || !servicoIndisponivel(ex)) {
            return Mono.error(ex);
        }

        String servico = nomeDoServico(exchange);
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("type", "about:blank");
        corpo.put("title", "Serviço indisponível");
        corpo.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        corpo.put("detail", "O serviço " + servico + " está indisponível no momento. Tente novamente em instantes.");
        corpo.put("instance", exchange.getRequest().getPath().value());
        corpo.put("servico", servico);

        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(corpo);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

        resposta.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        resposta.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        DataBuffer buffer = resposta.bufferFactory().wrap(bytes);
        return resposta.writeWith(Mono.just(buffer));
    }

    /** Percorre a cadeia de causas procurando falha de rede, timeout ou circuito aberto. */
    static boolean servicoIndisponivel(Throwable ex) {
        Throwable atual = ex;
        for (int i = 0; atual != null && i < PROFUNDIDADE_MAXIMA; i++) {
            if (atual instanceof IOException
                    || atual instanceof TimeoutException
                    || atual instanceof CallNotPermittedException) {
                return true;
            }
            atual = atual.getCause();
        }
        return false;
    }

    /** O id da rota (ver application.yml) é o nome do microsserviço: auth-service ou patient-service. */
    private static String nomeDoServico(ServerWebExchange exchange) {
        Route rota = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return rota != null ? rota.getId() : "desconhecido";
    }
}
