package com.healthsys.monitoring.leito.dto;

import com.healthsys.monitoring.leito.Leito;
import com.healthsys.monitoring.leito.Setor;
import com.healthsys.monitoring.leito.StatusLeito;

import java.time.LocalDateTime;

public record LeitoResponse(
        Integer id,
        String codigo,
        Setor setor,
        StatusLeito status,
        Integer pacienteId,
        String pacienteNome,
        LocalDateTime ocupadoEm,
        String ocupadoPorNome,
        LocalDateTime atualizadoEm,
        String atualizadoPorNome,
        Long versao
) {
    public static LeitoResponse de(Leito l) {
        return new LeitoResponse(l.getId(), l.getCodigo(), l.getSetor(), l.getStatus(), l.getPacienteId(),
                l.getPacienteNome(), l.getOcupadoEm(), l.getOcupadoPorNome(), l.getAtualizadoEm(),
                l.getAtualizadoPorNome(), l.getVersao());
    }

    /** Para o administrador: a situação do leito sem dizer quem está nele (dado de saúde). */
    public LeitoResponse semPaciente() {
        return new LeitoResponse(id, codigo, setor, status, null, null, null, null, atualizadoEm, atualizadoPorNome, versao);
    }
}
