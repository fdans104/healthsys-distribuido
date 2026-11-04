package com.healthsys.patient.paciente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PacienteRepository extends JpaRepository<Paciente, Integer> {

    boolean existsByCpf(String cpf);

    /** CPF já usado por OUTRO paciente (usado na atualização). */
    boolean existsByCpfAndIdNot(String cpf, Integer id);

    List<Paciente> findAllByOrderByNomeAsc();

    List<Paciente> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    List<Paciente> findByCpfStartingWithOrderByNomeAsc(String prefixoCpf);
}
