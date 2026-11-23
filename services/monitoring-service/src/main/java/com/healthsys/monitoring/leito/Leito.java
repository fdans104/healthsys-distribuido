package com.healthsys.monitoring.leito;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * Um leito do hospital. O paciente é guardado pelo id e nome informados pelo patient-service
 * (sem chave estrangeira entre bancos de serviços diferentes, RNF09).
 */
@Entity
@Table(name = "leitos")
public class Leito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_leito")
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Setor setor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusLeito status;

    @Column(name = "id_paciente")
    private Integer pacienteId;

    @Column(name = "paciente_nome", length = 150)
    private String pacienteNome;

    @Column(name = "ocupado_em")
    private LocalDateTime ocupadoEm;

    @Column(name = "ocupado_por_nome", length = 150)
    private String ocupadoPorNome;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "atualizado_por_nome", length = 150)
    private String atualizadoPorNome;

    /** RN54: número de edição. Duas pessoas não conseguem internar no mesmo leito ao mesmo tempo. */
    @Version
    private Long versao;

    protected Leito() {
        // exigido pelo JPA
    }

    public Leito(String codigo, Setor setor, Profissional quem, LocalDateTime agora) {
        this.codigo = codigo;
        this.setor = setor;
        this.status = StatusLeito.LIVRE;
        this.atualizadoEm = agora;
        this.atualizadoPorNome = quem == null ? null : quem.nome();
    }

    public void ocupar(Integer pacienteId, String pacienteNome, Profissional quem, LocalDateTime agora) {
        this.status = StatusLeito.OCUPADO;
        this.pacienteId = pacienteId;
        this.pacienteNome = pacienteNome;
        this.ocupadoEm = agora;
        this.ocupadoPorNome = quem.nome();
        this.atualizadoEm = agora;
        this.atualizadoPorNome = quem.nome();
    }

    /** Alta do leito: o paciente sai e o leito vai para a higienização. */
    public void liberar(Profissional quem, LocalDateTime agora) {
        this.status = StatusLeito.HIGIENIZACAO;
        this.pacienteId = null;
        this.pacienteNome = null;
        this.ocupadoEm = null;
        this.ocupadoPorNome = null;
        this.atualizadoEm = agora;
        this.atualizadoPorNome = quem.nome();
    }

    public void marcarHigienizado(Profissional quem, LocalDateTime agora) {
        this.status = StatusLeito.LIVRE;
        this.atualizadoEm = agora;
        this.atualizadoPorNome = quem.nome();
    }

    public Integer getId() { return id; }
    public String getCodigo() { return codigo; }
    public Setor getSetor() { return setor; }
    public StatusLeito getStatus() { return status; }
    public Integer getPacienteId() { return pacienteId; }
    public String getPacienteNome() { return pacienteNome; }
    public LocalDateTime getOcupadoEm() { return ocupadoEm; }
    public String getOcupadoPorNome() { return ocupadoPorNome; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public String getAtualizadoPorNome() { return atualizadoPorNome; }
    public Long getVersao() { return versao; }
}
