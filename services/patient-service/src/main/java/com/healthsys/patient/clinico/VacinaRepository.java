package com.healthsys.patient.clinico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VacinaRepository extends JpaRepository<Vacina, Integer> {

    List<Vacina> findByPacienteIdOrderByDataAplicacaoDescIdDesc(Integer pacienteId);

    boolean existsByPacienteId(Integer pacienteId);
}
