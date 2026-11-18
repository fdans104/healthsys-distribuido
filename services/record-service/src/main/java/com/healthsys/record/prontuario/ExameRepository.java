package com.healthsys.record.prontuario;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ExameRepository extends MongoRepository<Exame, String> {

    List<Exame> findByPacienteIdOrderBySolicitadoEmDesc(Integer pacienteId);

    Optional<Exame> findByIdAndPacienteId(String id, Integer pacienteId);
}
