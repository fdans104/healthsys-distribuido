package com.healthsys.monitoring.sinais.dto;

import com.healthsys.monitoring.sinais.Alerta;
import com.healthsys.monitoring.sinais.GravidadeAlerta;
import com.healthsys.monitoring.sinais.TipoAlerta;

import java.time.LocalDateTime;

public record AlertaResponse(
        Integer id,
        String leitoCodigo,
        String setor,
        Integer pacienteId,
        String pacienteNome,
        TipoAlerta tipo,
        GravidadeAlerta gravidade,
        String valor,
        String mensagem,
        LocalDateTime criadoEm,
        boolean ativo,
        LocalDateTime reconhecidoEm,
        String reconhecidoPorNome
) {
    public static AlertaResponse de(Alerta a) {
        return new AlertaResponse(a.getId(), a.getLeitoCodigo(), a.getSetor(), a.getPacienteId(), a.getPacienteNome(),
                a.getTipo(), a.getGravidade(), a.getValor(), a.getMensagem(), a.getCriadoEm(), a.ativo(),
                a.getReconhecidoEm(), a.getReconhecidoPorNome());
    }
}
