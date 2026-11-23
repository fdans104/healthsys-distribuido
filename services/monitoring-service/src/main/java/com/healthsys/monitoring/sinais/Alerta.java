package com.healthsys.monitoring.sinais;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Um alerta de sinal vital. Fica ativo até um médico ou enfermeiro reconhecer que viu. */
@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer id;

    @Column(name = "leito_codigo", nullable = false, length = 20)
    private String leitoCodigo;

    @Column(length = 20)
    private String setor;

    @Column(name = "id_paciente", nullable = false)
    private Integer pacienteId;

    @Column(name = "paciente_nome", length = 150)
    private String pacienteNome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAlerta tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GravidadeAlerta gravidade;

    @Column(length = 30)
    private String valor;

    @Column(nullable = false, length = 300)
    private String mensagem;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "reconhecido_em")
    private LocalDateTime reconhecidoEm;

    @Column(name = "reconhecido_por_nome", length = 150)
    private String reconhecidoPorNome;

    protected Alerta() {
        // exigido pelo JPA
    }

    public Alerta(String leitoCodigo, String setor, Integer pacienteId, String pacienteNome, Achado achado,
                  LocalDateTime agora) {
        this.leitoCodigo = leitoCodigo;
        this.setor = setor;
        this.pacienteId = pacienteId;
        this.pacienteNome = pacienteNome;
        this.tipo = achado.tipo();
        this.gravidade = achado.gravidade();
        this.valor = achado.valor();
        this.mensagem = achado.mensagem();
        this.criadoEm = agora;
    }

    public void reconhecer(String quem, LocalDateTime agora) {
        this.reconhecidoEm = agora;
        this.reconhecidoPorNome = quem;
    }

    public boolean ativo() {
        return reconhecidoEm == null;
    }

    public Integer getId() { return id; }
    public String getLeitoCodigo() { return leitoCodigo; }
    public String getSetor() { return setor; }
    public Integer getPacienteId() { return pacienteId; }
    public String getPacienteNome() { return pacienteNome; }
    public TipoAlerta getTipo() { return tipo; }
    public GravidadeAlerta getGravidade() { return gravidade; }
    public String getValor() { return valor; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getReconhecidoEm() { return reconhecidoEm; }
    public String getReconhecidoPorNome() { return reconhecidoPorNome; }
}
