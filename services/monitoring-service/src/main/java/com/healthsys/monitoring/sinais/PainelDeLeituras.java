package com.healthsys.monitoring.sinais;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda em memória a última leitura de cada leito (os monitores mandam uma a cada poucos segundos;
 * não faz sentido gravar todas no banco). Só os alertas são gravados.
 * Se um leito ocupado fica mais de 15 segundos sem mandar leitura, a tela mostra "sem sinal do monitor".
 */
@Component
public class PainelDeLeituras {

    public static final Duration TEMPO_SEM_SINAL = Duration.ofSeconds(15);

    private final Map<String, LeituraRecebida> ultimas = new ConcurrentHashMap<>();

    public void registrar(String codigoLeito, LeituraSinais leitura, Instant agora) {
        ultimas.put(codigoLeito, new LeituraRecebida(leitura, agora));
    }

    public Optional<LeituraRecebida> ultima(String codigoLeito) {
        return Optional.ofNullable(ultimas.get(codigoLeito));
    }

    public void descartar(String codigoLeito) {
        ultimas.remove(codigoLeito);
    }

    public static boolean semSinal(LeituraRecebida recebida, Instant agora) {
        return recebida == null || recebida.recebidaEm().plus(TEMPO_SEM_SINAL).isBefore(agora);
    }
}
