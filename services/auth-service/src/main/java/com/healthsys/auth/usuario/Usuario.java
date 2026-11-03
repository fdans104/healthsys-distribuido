package com.healthsys.auth.usuario;

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

/** Mapeia a tabela "usuarios" criada em infra/db/postgres-init/init.sql. */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // SERIAL no Postgres
    @Column(name = "id_usuario")
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Perfil perfil;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Hash BCrypt da senha (nunca a senha original). */
    @Column(name = "senha", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    protected Usuario() {
        // exigido pelo JPA
    }

    public Usuario(String nome, Perfil perfil, String email, String senhaHash) {
        this.nome = nome;
        this.perfil = perfil;
        this.email = email;
        this.senhaHash = senhaHash;
    }

    @PrePersist
    void aoCriar() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public Perfil getPerfil() { return perfil; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public void setNome(String nome) { this.nome = nome; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
    public void setEmail(String email) { this.email = email; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
}
