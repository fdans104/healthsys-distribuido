package com.healthsys.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicoIndisponivelHandlerTest {

    private final ServicoIndisponivelHandler handler = new ServicoIndisponivelHandler(new ObjectMapper());

    private static MockServerWebExchange novaRequisicao() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/pacientes").build());
    }

    private void verificaResposta503(MockServerWebExchange exchange) {
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(503);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        String corpo = exchange.getResponse().getBodyAsString().block();
        assertThat(corpo).contains("\"status\":503").contains("indisponível").contains("/api/v1/pacientes");
    }

    @Test
    void hostNaoResolvidoViraResposta503() {
        MockServerWebExchange exchange = novaRequisicao();

        handler.handle(exchange, new UnknownHostException("Failed to resolve 'patient-service'")).block();

        verificaResposta503(exchange);
    }

    @Test
    void conexaoRecusadaEmCausaInternaViraResposta503() {
        MockServerWebExchange exchange = novaRequisicao();

        handler.handle(exchange, new RuntimeException("erro de rede", new ConnectException("recusada"))).block();

        verificaResposta503(exchange);
    }

    @Test
    void timeoutViraResposta503() {
        MockServerWebExchange exchange = novaRequisicao();

        handler.handle(exchange, new TimeoutException()).block();

        verificaResposta503(exchange);
    }

    @Test
    void outrosErrosNaoSaoTratadosAqui() {
        MockServerWebExchange exchange = novaRequisicao();

        assertThatThrownBy(() -> handler.handle(exchange, new IllegalStateException("bug")).block())
                .isInstanceOf(IllegalStateException.class);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }
}
