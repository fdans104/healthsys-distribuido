package com.healthsys.patient.clinico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Integer> {

    List<Atendimento> findByPacienteIdOrderByDataDescIdDesc(Integer pacienteId);

    boolean existsByPacienteId(Integer pacienteId);
}
