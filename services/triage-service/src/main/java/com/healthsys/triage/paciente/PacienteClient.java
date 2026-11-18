package com.healthsys.triage.paciente;

import com.healthsys.triage.exception.RecursoNaoEncontradoException;
import com.healthsys.triage.exception.ServicoIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Comunicação síncrona (REST, RNF04) com o patient-service.
 * RNF09: a triagem não lê o banco de pacientes; ele pergunta ao serviço dono desses dados.
 * O token do usuário é repassado, então o patient-service aplica as próprias regras de acesso.
 */
@Component
public class PacienteClient {

    private final RestClient http;

    public PacienteClient(RestClient.Builder builder, @Value("${servicos.pacientes-url}") String urlPacientes) {
        SimpleClientHttpRequestFactory tempos = new SimpleClientHttpRequestFactory();
        tempos.setConnectTimeout(3000);
        tempos.setReadTimeout(5000);
        this.http = builder.baseUrl(urlPacientes).requestFactory(tempos).build();
    }

    /** Confirma que o paciente existe. Lança 404 se não existir e 503 se o serviço de pacientes não responder. */
    public PacienteResumo buscar(Integer pacienteId, String authorization) {
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
        } catch (RestClientException e) {
            throw new ServicoIndisponivelException(
                    "Não foi possível confirmar o paciente: o serviço de pacientes não respondeu. Tente novamente em instantes.");
        }
    }
}
