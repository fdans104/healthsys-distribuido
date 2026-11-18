package com.healthsys.triage.triagem.dto;

import com.healthsys.triage.triagem.ClassificacaoRisco;
import com.healthsys.triage.triagem.StatusTriagem;
import com.healthsys.triage.triagem.Triagem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TriagemResponse(
        Integer id,
        Integer pacienteId,
        String pacienteNome,
        String queixaPrincipal,
        String sintomas,
        String pressaoArterial,
        BigDecimal temperatura,
        Integer frequenciaCardiaca,
        Integer saturacao,
        ClassificacaoRisco classificacaoRisco,
        StatusTriagem status,
        LocalDateTime abertaEm,
        String abertaPorNome,
        String abertaPorPerfil,
        LocalDateTime avaliadaEm,
        String avaliadaPorNome,
        LocalDateTime atualizadaEm,
        String atualizadaPorNome,
        Long versao
) {
    public static TriagemResponse de(Triagem t) {
        return new TriagemResponse(t.getId(), t.getPacienteId(), t.getPacienteNome(), t.getQueixaPrincipal(),
                t.getSintomas(), t.getPressaoArterial(), t.getTemperatura(), t.getFrequenciaCardiaca(), t.getSaturacao(),
                t.getClassificacaoRisco(), t.getStatus(), t.getAbertaEm(), t.getAbertaPorNome(), t.getAbertaPorPerfil(),
                t.getAvaliadaEm(), t.getAvaliadaPorNome(), t.getAtualizadaEm(), t.getAtualizadaPorNome(), t.getVersao());
    }

    /**
     * Versão para a recepção: mostra a posição na fila e o risco, mas não os sintomas
     * nem os sinais vitais, que são dado clínico (RN18, RNF17).
     */
    public TriagemResponse semDadosClinicos() {
        return new TriagemResponse(id, pacienteId, pacienteNome, queixaPrincipal,
                null, null, null, null, null,
                classificacaoRisco, status, abertaEm, abertaPorNome, abertaPorPerfil,
                avaliadaEm, avaliadaPorNome, atualizadaEm, atualizadaPorNome, versao);
    }
}
