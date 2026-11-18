package com.healthsys.record.prontuario;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ConsultaRepository extends MongoRepository<Consulta, String> {

    List<Consulta> findByPacienteIdOrderByDataDesc(Integer pacienteId);
}
