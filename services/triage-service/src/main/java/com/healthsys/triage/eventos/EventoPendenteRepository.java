package com.healthsys.triage.eventos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface EventoPendenteRepository extends JpaRepository<EventoPendente, Long> {

    /** Os mais antigos primeiro, para manter a ordem dos acontecimentos. */
    List<EventoPendente> findTop50ByEnviadoEmIsNullOrderByIdAsc();

    long countByEnviadoEmIsNull();

    @Modifying
    @Transactional
    @Query("delete from EventoPendente e where e.enviadoEm is not null and e.enviadoEm < :limite")
    int apagarEnviadosAntesDe(@Param("limite") Instant limite);
}
