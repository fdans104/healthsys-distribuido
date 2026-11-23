package com.healthsys.record.paciente;

import com.healthsys.record.exception.RecursoNaoEncontradoException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Comunicação síncrona (REST, RNF04) com o patient-service: confirma o paciente antes de gravar no prontuário.
 * RNF09: este serviço não lê o banco de pacientes; ele pergunta ao serviço dono desses dados.
 * O token do usuário é repassado, então o patient-service aplica as próprias regras de acesso.
 * A chamada é protegida por timeout, nova tentativa e circuit breaker (ChamadaResiliente).
 */
@Component
public class PacienteClient {

    private final RestClient http;
    private final CircuitBreaker circuito;
    private final Retry novaTentativa;

    public PacienteClient(RestClient.Builder builder, @Value("${servicos.pacientes-url}") String urlPacientes,
                          CircuitBreaker circuitoPacientes, Retry novaTentativaPacientes) {
        SimpleClientHttpRequestFactory tempos = new SimpleClientHttpRequestFactory();
        tempos.setConnectTimeout(2000);
        tempos.setReadTimeout(3000);
        this.http = builder.baseUrl(urlPacientes).requestFactory(tempos).build();
        this.circuito = circuitoPacientes;
        this.novaTentativa = novaTentativaPacientes;
    }

    /** Confirma que o paciente existe. Lança 404 se não existir e 503 se o serviço de pacientes não responder. */
    public PacienteResumo buscar(Integer pacienteId, String authorization) {
        return ChamadaResiliente.executar(circuito, novaTentativa, () -> consultar(pacienteId, authorization));
    }

    private PacienteResumo consultar(Integer pacienteId, String authorization) {
        try {
            PacienteResumo paciente = http.get()
                    .uri("/api/v1/pacientes/{id}", pacienteId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(PacienteResumo.class);
            if (paciente == null) {
                throw new RecursoNaoEncontradoException("Paciente " + pacienteId + " não encontrado");
            }
            return paciente;
        } catch (HttpClientErrorException.NotFound e) {
            throw new RecursoNaoEncontradoException("Paciente " + pacienteId + " não encontrado");
        }
    }
}
