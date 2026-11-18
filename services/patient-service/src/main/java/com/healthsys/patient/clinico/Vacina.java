package com.healthsys.patient.clinico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** RF09: vacina aplicada em um paciente. Registro de prontuário: não é editado nem apagado. */
@Entity
@Table(name = "vacinas")
public class Vacina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vacina")
    private Integer id;

    @Column(name = "id_paciente", nullable = false, updatable = false)
    private Integer pacienteId;

    @Column(name = "nome_vacina", nullable = false, length = 100)
    private String nomeVacina;

    @Column(name = "data_aplicacao", nullable = false)
    private LocalDate dataAplicacao;

    /** Ex.: "1ª dose", "reforço". Opcional. */
    @Column(length = 30)
    private String dose;

    /** Lote do frasco aplicado. Opcional. */
    @Column(length = 30)
    private String lote;

    @Column(name = "registrado_em", updatable = false)
    private LocalDateTime registradoEm;

    @Column(name = "registrado_por_id", updatable = false)
    private Integer registradoPorId;

    @Column(name = "registrado_por_nome", length = 150, updatable = false)
    private String registradoPorNome;

    @Column(name = "registrado_por_perfil", length = 30, updatable = false)
    private String registradoPorPerfil;

    protected Vacina() {
        // exigido pelo JPA
    }

    public Vacina(Integer pacienteId, String nomeVacina, LocalDate dataAplicacao, String dose, String lote,
                  Profissional profissional) {
        this.pacienteId = pacienteId;
        this.nomeVacina = nomeVacina;
        this.dataAplicacao = dataAplicacao;
        this.dose = dose;
        this.lote = lote;
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
    public String getNomeVacina() { return nomeVacina; }
    public LocalDate getDataAplicacao() { return dataAplicacao; }
    public String getDose() { return dose; }
    public String getLote() { return lote; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public Integer getRegistradoPorId() { return registradoPorId; }
    public String getRegistradoPorNome() { return registradoPorNome; }
    public String getRegistradoPorPerfil() { return registradoPorPerfil; }
}
