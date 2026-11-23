package com.healthsys.triage.triagem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TriagemRepository extends JpaRepository<Triagem, Integer> {

    List<Triagem> findByStatus(StatusTriagem status);

    List<Triagem> findByPacienteIdOrderByAbertaEmDesc(Integer pacienteId);

    /** Usado para não abrir duas triagens ao mesmo tempo para o mesmo paciente. */
    boolean existsByPacienteIdAndStatusIn(Integer pacienteId, Collection<StatusTriagem> status);

    /** Usados nas métricas da fila (Prometheus/Grafana). */
    long countByStatusInAndClassificacaoRisco(Collection<StatusTriagem> status, ClassificacaoRisco risco);

    long countByStatusInAndClassificacaoRiscoIsNull(Collection<StatusTriagem> status);
}
