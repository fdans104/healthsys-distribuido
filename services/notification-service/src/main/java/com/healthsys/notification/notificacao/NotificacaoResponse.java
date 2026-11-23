package com.healthsys.notification.notificacao;

import java.time.Instant;

/** O que a tela recebe: a notificação e se a pessoa logada já leu. */
public record NotificacaoResponse(
        String id,
        String tipo,
        String titulo,
        String mensagem,
        Gravidade gravidade,
        Integer pacienteId,
        String pacienteNome,
        String secao,
        Instant criadaEm,
        boolean lida
) {
    public static NotificacaoResponse de(Notificacao n, Integer usuarioId) {
        return new NotificacaoResponse(n.getId(), n.getTipo(), n.getTitulo(), n.getMensagem(), n.getGravidade(),
                n.getPacienteId(), n.getPacienteNome(), n.getSecao(), n.getCriadaEm(), n.lidaPor(usuarioId));
    }
}
