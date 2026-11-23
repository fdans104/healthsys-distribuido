package com.healthsys.triage.paciente;

import com.healthsys.triage.exception.RecursoNaoEncontradoException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;

/**
 * Configuração do circuit breaker e das novas tentativas para o patient-service.
 * Os registros vêm do Resilience4j, que já publica o estado no Prometheus
 * (métrica resilience4j_circuitbreaker_state, painel no Grafana).
 */
@Configuration
public class ResilienciaPacientesConfig {

    public static final String NOME = "pacientes";

    @Bean
    public CircuitBreaker circuitoPacientes(CircuitBreakerRegistry registro) {
        return registro.circuitBreaker(NOME, configuracaoCircuito(Duration.ofSeconds(10)));
    }

    @Bean
    public Retry novaTentativaPacientes(RetryRegistry registro) {
        return registro.retry(NOME, configuracaoNovaTentativa(Duration.ofMillis(300)));
    }

    /** esperaAberto: quanto tempo o circuito fica aberto antes de testar de novo (10 s; nos testes, menos). */
    static CircuitBreakerConfig configuracaoCircuito(Duration esperaAberto) {
        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(esperaAberto)
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // Respostas 4xx (ex.: paciente não existe) não indicam que o serviço caiu.
                .ignoreExceptions(RecursoNaoEncontradoException.class, HttpClientErrorException.class)
                .build();
    }

    /** espera: intervalo entre as tentativas (300 ms; nos testes, 1 ms). */
    static RetryConfig configuracaoNovaTentativa(Duration espera) {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(espera)
                // Só repete quando a falha é de comunicação (conexão recusada, timeout) ou erro 5xx.
                .retryExceptions(ResourceAccessException.class, HttpServerErrorException.class)
                .build();
    }
}
