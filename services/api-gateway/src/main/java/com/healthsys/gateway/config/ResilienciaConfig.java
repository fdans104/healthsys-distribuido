package com.healthsys.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit breaker de cada rota do gateway.
 * - Se um serviço não responde (fora do ar / timeout), o gateway devolve 503 na hora (ServicoIndisponivelHandler)
 *   em vez de deixar o usuário esperando.
 * - Com 50% de falhas nas últimas 10 chamadas o circuito "abre" e para de chamar o serviço por 10s,
 *   dando tempo para ele se recuperar; depois testa com 2 chamadas ("meio aberto").
 * Respostas HTTP de erro (400, 401, 404...) NÃO contam como falha: só falhas de comunicação contam.
 */
@Configuration
public class ResilienciaConfig {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> configuracaoPadrao() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .build())
                // O padrão do Resilience4j é 1s, curto demais para o login (BCrypt) e para a 1ª chamada após subir.
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build())
                .build());
    }
}
