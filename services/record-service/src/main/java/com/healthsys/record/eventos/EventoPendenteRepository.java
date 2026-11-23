package com.healthsys.record.eventos;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface EventoPendenteRepository extends MongoRepository<EventoPendente, String> {

    /** Os mais antigos primeiro, para manter a ordem dos acontecimentos. */
    List<EventoPendente> findTop50ByEnviadoEmIsNullOrderByCriadoEmAsc();

    long countByEnviadoEmIsNull();

    long deleteByEnviadoEmBefore(Instant limite);
}
