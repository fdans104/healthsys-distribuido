package com.healthsys.triage.paciente;

import com.healthsys.triage.exception.RecursoNaoEncontradoException;
import com.healthsys.triage.exception.ServicoIndisponivelException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testes de tolerância a falhas da chamada ao patient-service (sem precisar subir nada). */
class ChamadaResilienteTest {

    /** Mesmas regras da configuração real, só que sem esperar 300 ms entre as tentativas. */
    private static Retry novaTentativa() {
        return Retry.of("teste", ResilienciaPacientesConfig.configuracaoNovaTentativa(Duration.ofMillis(1)));
    }

    private static CircuitBreaker circuito() {
        return CircuitBreaker.of("teste", ResilienciaPacientesConfig.configuracaoCircuito(Duration.ofSeconds(10)));
    }

    @Test
    void quandoOServicoVoltaNaSegundaTentativaAChamadaDaCerto() {
        AtomicInteger chamadas = new AtomicInteger();
        String resposta = ChamadaResiliente.executar(circuito(), novaTentativa(), () -> {
            if (chamadas.incrementAndGet() == 1) {
                throw new ResourceAccessException("conexão recusada");
            }
            return "ok";
        });

        assertThat(resposta).isEqualTo("ok");
        assertThat(chamadas.get()).isEqualTo(2);
    }

    @Test
    void servicoForaDoArViraErro503DepoisDeTresTentativas() {
        AtomicInteger chamadas = new AtomicInteger();

        assertThatThrownBy(() -> ChamadaResiliente.executar(circuito(), novaTentativa(), () -> {
            chamadas.incrementAndGet();
            throw new ResourceAccessException("conexão recusada");
        })).isInstanceOf(ServicoIndisponivelException.class);

        assertThat(chamadas.get()).isEqualTo(3);
    }

    @Test
    void depoisDeVariasFalhasOCircuitoAbreEParaDeChamarOServico() {
        CircuitBreaker circuito = circuito();
        Retry novaTentativa = novaTentativa();
        AtomicInteger chamadas = new AtomicInteger();

        // 5 chamadas que falham (mínimo para o circuito avaliar), cada uma com 3 tentativas.
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> ChamadaResiliente.executar(circuito, novaTentativa, () -> {
                chamadas.incrementAndGet();
                throw new ResourceAccessException("timeout");
            })).isInstanceOf(ServicoIndisponivelException.class);
        }
        assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(chamadas.get()).isEqualTo(15);

        // Com o circuito aberto, a resposta é imediata e o serviço nem é chamado.
        assertThatThrownBy(() -> ChamadaResiliente.executar(circuito, novaTentativa, () -> {
            chamadas.incrementAndGet();
            return "não deveria chamar";
        })).isInstanceOf(ServicoIndisponivelException.class)
                .hasMessageContaining("circuit breaker");
        assertThat(chamadas.get()).isEqualTo(15);
    }

    @Test
    void pacienteInexistenteNaoContaComoFalhaNemERepetido() {
        CircuitBreaker circuito = circuito();
        AtomicInteger chamadas = new AtomicInteger();

        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> ChamadaResiliente.executar(circuito, novaTentativa(), () -> {
                chamadas.incrementAndGet();
                throw new RecursoNaoEncontradoException("Paciente 99 não encontrado");
            })).isInstanceOf(RecursoNaoEncontradoException.class);
        }

        assertThat(chamadas.get()).isEqualTo(10);
        assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void depoisDoTempoDeEsperaOCircuitoTestaDeNovoEFechaSeOServicoVoltou() {
        CircuitBreaker circuito = CircuitBreaker.of("teste",
                ResilienciaPacientesConfig.configuracaoCircuito(Duration.ofMillis(50)));
        Retry novaTentativa = novaTentativa();

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> ChamadaResiliente.executar(circuito, novaTentativa, () -> {
                throw new ResourceAccessException("fora do ar");
            })).isInstanceOf(ServicoIndisponivelException.class);
        }
        assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        esperar(120);
        // Meio aberto: 2 chamadas de teste dão certo e o circuito fecha.
        assertThat(ChamadaResiliente.executar(circuito, novaTentativa, () -> "voltou")).isEqualTo("voltou");
        assertThat(ChamadaResiliente.executar(circuito, novaTentativa, () -> "voltou")).isEqualTo("voltou");
        assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    private static void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
