package com.healthsys.monitoring.leito;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeitoRepository extends JpaRepository<Leito, Integer> {

    List<Leito> findAllByOrderBySetorAscCodigoAsc();

    List<Leito> findByStatusOrderBySetorAscCodigoAsc(StatusLeito status);

    Optional<Leito> findByCodigo(String codigo);

    Optional<Leito> findByPacienteId(Integer pacienteId);

    boolean existsByCodigo(String codigo);

    long countBySetorAndStatus(Setor setor, StatusLeito status);
}
