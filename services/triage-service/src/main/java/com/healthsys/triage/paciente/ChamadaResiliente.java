package com.healthsys.triage.paciente;

import com.healthsys.triage.exception.RecursoNaoEncontradoException;
import com.healthsys.triage.exception.ServicoIndisponivelException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import java.util.function.Supplier;

/**
 * Tolerância a falhas na comunicação com outro microsserviço (Resilience4j):
 * - Retry (por dentro): se a conexão falha ou o serviço responde 5xx, tenta de novo (3 vezes no total).
 * - Circuit breaker (por fora): se metade das últimas chamadas falhou, o circuito ABRE e,
 *   por 10 segundos, nem tenta chamar: responde na hora que o serviço está indisponível.
 *   Depois deixa passar 2 chamadas de teste; se derem certo, o circuito FECHA de novo.
 * - "Paciente não encontrado" (404) não é falha de comunicação: não abre o circuito nem repete.
 */
public final class ChamadaResiliente {

    private ChamadaResiliente() {
    }

    public static <T> T executar(CircuitBreaker circuito, Retry novaTentativa, Supplier<T> chamada) {
        Supplier<T> protegida = CircuitBreaker.decorateSupplier(circuito, Retry.decorateSupplier(novaTentativa, chamada));
        try {
            return protegida.get();
        } catch (RecursoNaoEncontradoException e) {
            throw e;
        } catch (CallNotPermittedException e) {
            throw new ServicoIndisponivelException(
                    "O serviço de pacientes está fora do ar e a proteção (circuit breaker) foi ativada. "
                            + "Tente novamente em alguns segundos.");
        } catch (RuntimeException e) {
            throw new ServicoIndisponivelException(
                    "Não foi possível confirmar o paciente: o serviço de pacientes não respondeu. Tente novamente em instantes.");
        }
    }
}
