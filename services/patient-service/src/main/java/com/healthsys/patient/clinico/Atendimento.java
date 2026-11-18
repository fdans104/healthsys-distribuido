package com.healthsys.patient.clinico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** RF06: atendimento médico de um paciente. Registro de prontuário: não é editado nem apagado. */
@Entity
@Table(name = "atendimentos")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_atendimento")
    private Integer id;

    @Column(name = "id_paciente", nullable = false, updatable = false)
    private Integer pacienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_atendimento", nullable = false, length = 50)
    private TipoAtendimento tipo;

    /** Data e hora em que o atendimento aconteceu (RN16). */
    @Column(name = "data", nullable = false)
    private LocalDateTime data;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "registrado_em", updatable = false)
    private LocalDateTime registradoEm;

    @Column(name = "registrado_por_id", updatable = false)
    private Integer registradoPorId;

    @Column(name = "registrado_por_nome", length = 150, updatable = false)
    private String registradoPorNome;

    @Column(name = "registrado_por_perfil", length = 30, updatable = false)
    private String registradoPorPerfil;

    protected Atendimento() {
        // exigido pelo JPA
    }

    public Atendimento(Integer pacienteId, TipoAtendimento tipo, LocalDateTime data, String observacoes,
                       Profissional profissional) {
        this.pacienteId = pacienteId;
        this.tipo = tipo;
        this.data = data;
        this.observacoes = observacoes;
        this.registradoPorId = profissional.id();
        this.registradoPorNome = profissional.nome();
        this.registradoPorPerfil = profissional.perfil();
    }

    @PrePersist
    void aoCriar() {
        if (registradoEm == null) {
            registradoEm = LocalDateTime.now();
        }
    }

    public Integer getId() { return id; }
    public Integer getPacienteId() { return pacienteId; }
    public TipoAtendimento getTipo() { return tipo; }
    public LocalDateTime getData() { return data; }
    public String getObservacoes() { return observacoes; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public Integer getRegistradoPorId() { return registradoPorId; }
    public String getRegistradoPorNome() { return registradoPorNome; }
    public String getRegistradoPorPerfil() { return registradoPorPerfil; }
}
