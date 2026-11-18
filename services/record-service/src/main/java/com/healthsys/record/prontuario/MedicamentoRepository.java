package com.healthsys.record.prontuario;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MedicamentoRepository extends MongoRepository<Medicamento, String> {

    List<Medicamento> findByPacienteIdOrderByPrescritoEmDesc(Integer pacienteId);

    Optional<Medicamento> findByIdAndPacienteId(String id, Integer pacienteId);
}
