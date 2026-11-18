package com.healthsys.patient.clinico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** RF08: alergia de um paciente. Registro de prontuário: não é editado nem apagado. */
@Entity
@Table(name = "alergias")
public class Alergia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alergia")
    private Integer id;

    @Column(name = "id_paciente", nullable = false, updatable = false)
    private Integer pacienteId;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime registradoEm;

    @Column(name = "registrado_por_id", updatable = false)
    private Integer registradoPorId;

    @Column(name = "registrado_por_nome", length = 150, updatable = false)
    private String registradoPorNome;

    @Column(name = "registrado_por_perfil", length = 30, updatable = false)
    private String registradoPorPerfil;

    protected Alergia() {
        // exigido pelo JPA
    }

    public Alergia(Integer pacienteId, String descricao, Profissional profissional) {
        this.pacienteId = pacienteId;
        this.descricao = descricao;
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
    public String getDescricao() { return descricao; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public Integer getRegistradoPorId() { return registradoPorId; }
    public String getRegistradoPorNome() { return registradoPorNome; }
    public String getRegistradoPorPerfil() { return registradoPorPerfil; }
}
