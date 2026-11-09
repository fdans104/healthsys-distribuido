package com.healthsys.patient.paciente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Mapeia a tabela "pacientes" (infra/db/postgres-init/init.sql e schema.sql deste serviço). */
@Entity
@Table(name = "pacientes")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // SERIAL no Postgres
    @Column(name = "id_paciente")
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nome;

    /** Somente os 11 dígitos, sem pontuação. Único na tabela. */
    @Column(length = 11, unique = true)
    private String cpf;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Sexo sexo;

    @Column(length = 20)
    private String telefone;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String endereco;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    /** RN54: número de edição. O Hibernate soma +1 a cada alteração e recusa gravar sobre uma versão antiga. */
    @Version
    private Long versao;

    protected Paciente() {
        // exigido pelo JPA
    }

    public Paciente(String nome, String cpf, LocalDate dataNascimento, Sexo sexo,
                    String telefone, String email, String endereco) {
        this.nome = nome;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.sexo = sexo;
        this.telefone = telefone;
        this.email = email;
        this.endereco = endereco;
    }

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public Sexo getSexo() { return sexo; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public String getEndereco() { return endereco; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public Long getVersao() { return versao; }

    public void setNome(String nome) { this.nome = nome; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public void setSexo(Sexo sexo) { this.sexo = sexo; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setEmail(String email) { this.email = email; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
}