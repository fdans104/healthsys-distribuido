package com.healthsys.monitoring.sinais;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Evita "chuva de alertas": o monitor manda uma leitura a cada 3 segundos, mas a equipe só deve ser avisada
 * quando algo NOVO acontece.
 * - Já existe alerta aberto do mesmo tipo para o mesmo paciente: não repete (só se piorou de ATENÇÃO para CRÍTICO).
 * - Alguém reconheceu há menos de 5 minutos: não repete (só se piorou).
 * - Paciente diferente no leito, ou nenhum alerta anterior: avisa.
 */
public final class RegraDeRepeticao {

    public static final Duration SILENCIO_APOS_RECONHECER = Duration.ofMinutes(5);

    private RegraDeRepeticao() {
    }

    public static boolean deveAlertar(Alerta ultimo, Integer pacienteAtual, GravidadeAlerta nova, LocalDateTime agora) {
        if (ultimo == null || !Objects.equals(ultimo.getPacienteId(), pacienteAtual)) {
            return true;
        }
        boolean piorou = ultimo.getGravidade() == GravidadeAlerta.ATENCAO && nova == GravidadeAlerta.CRITICO;
        if (piorou) {
            return true;
        }
        if (ultimo.ativo()) {
            return false;
        }
        return ultimo.getReconhecidoEm().plus(SILENCIO_APOS_RECONHECER).isBefore(agora);
    }
}
