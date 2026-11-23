package com.healthsys.monitoring.leito.dto;

import com.healthsys.monitoring.leito.Setor;

/** Painel de ocupação hospitalar: quantos leitos de cada setor estão livres, ocupados ou em limpeza. */
public record OcupacaoSetorResponse(
        Setor setor,
        long total,
        long livres,
        long ocupados,
        long emHigienizacao,
        int taxaOcupacao
) {
    public static OcupacaoSetorResponse de(Setor setor, long livres, long ocupados, long emHigienizacao) {
        long total = livres + ocupados + emHigienizacao;
        int taxa = total == 0 ? 0 : (int) Math.round(ocupados * 100.0 / total);
        return new OcupacaoSetorResponse(setor, total, livres, ocupados, emHigienizacao, taxa);
    }
}
