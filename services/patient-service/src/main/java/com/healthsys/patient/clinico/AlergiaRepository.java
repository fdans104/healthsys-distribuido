package com.healthsys.patient.clinico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlergiaRepository extends JpaRepository<Alergia, Integer> {

    List<Alergia> findByPacienteIdOrderByRegistradoEmDescIdDesc(Integer pacienteId);

    boolean existsByPacienteId(Integer pacienteId);
}
