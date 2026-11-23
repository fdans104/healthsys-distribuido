package com.healthsys.monitoring.sinais;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    /** O alerta mais recente deste tipo neste leito (para não repetir o mesmo aviso a cada leitura). */
    Optional<Alerta> findFirstByLeitoCodigoAndTipoOrderByCriadoEmDesc(String leitoCodigo, TipoAlerta tipo);

    List<Alerta> findByReconhecidoEmIsNullOrderByCriadoEmDesc();

    List<Alerta> findTop100ByOrderByCriadoEmDesc();

    long countByLeitoCodigoAndReconhecidoEmIsNull(String leitoCodigo);

    /** Na alta do leito, os alertas ainda abertos daquele paciente são encerrados. */
    @Modifying
    @Query("update Alerta a set a.reconhecidoEm = :agora, a.reconhecidoPorNome = :motivo "
            + "where a.leitoCodigo = :codigo and a.reconhecidoEm is null")
    int encerrarAtivosDoLeito(@Param("codigo") String codigo, @Param("agora") LocalDateTime agora,
                              @Param("motivo") String motivo);
}
