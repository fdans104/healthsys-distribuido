package com.healthsys.triage.triagem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RF14: triagem de um paciente no pronto-atendimento.
 * A recepção abre (status PENDENTE), a enfermagem avalia e classifica o risco, o médico atende e finaliza.
 * O nome do paciente é guardado na abertura, porque este serviço não lê o banco de pacientes (RNF09).
 */
@Entity
@Table(name = "triagens")
public class Triagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_triagem")
    private Integer id;

    @Column(name = "id_paciente", nullable = false, updatable = false)
    private Integer pacienteId;

    @Column(name = "paciente_nome", length = 150, updatable = false)
    private String pacienteNome;

    @Column(name = "queixa_principal", length = 500, nullable = false)
    private String queixaPrincipal;

    @Column(columnDefinition = "TEXT")
    private String sintomas;

    @Column(name = "pressao_arterial", length = 10)
    private String pressaoArterial;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperatura;

    @Column(name = "frequencia_cardiaca")
    private Integer frequenciaCardiaca;

    private Integer saturacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao_risco", length = 10)
    private ClassificacaoRisco classificacaoRisco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTriagem status;

    @Column(name = "aberta_em", nullable = false, updatable = false)
    private LocalDateTime abertaEm;

    @Column(name = "aberta_por_nome", length = 150, updatable = false)
    private String abertaPorNome;

    @Column(name = "aberta_por_perfil", length = 30, updatable = false)
    private String abertaPorPerfil;

    @Column(name = "avaliada_em")
    private LocalDateTime avaliadaEm;

    @Column(name = "avaliada_por_nome", length = 150)
    private String avaliadaPorNome;

    @Column(name = "atualizada_em")
    private LocalDateTime atualizadaEm;

    @Column(name = "atualizada_por_nome", length = 150)
    private String atualizadaPorNome;

    /** RN54: número de edição. Quem salva com um número antigo é recusado. */
    @Version
    private Long versao;

    protected Triagem() {
        // exigido pelo JPA
    }

    public Triagem(Integer pacienteId, String pacienteNome, String queixaPrincipal, Profissional quemAbriu,
                   LocalDateTime agora) {
        this.pacienteId = pacienteId;
        this.pacienteNome = pacienteNome;
        this.queixaPrincipal = queixaPrincipal;
        this.status = StatusTriagem.PENDENTE;
        this.abertaEm = agora;
        this.abertaPorNome = quemAbriu.nome();
        this.abertaPorPerfil = quemAbriu.perfil();
        this.atualizadaEm = agora;
        this.atualizadaPorNome = quemAbriu.nome();
    }

    public void avaliar(String sintomas, String pressaoArterial, BigDecimal temperatura, Integer frequenciaCardiaca,
                        Integer saturacao, ClassificacaoRisco risco, Profissional enfermeiro, LocalDateTime agora) {
        this.sintomas = sintomas;
        this.pressaoArterial = pressaoArterial;
        this.temperatura = temperatura;
        this.frequenciaCardiaca = frequenciaCardiaca;
        this.saturacao = saturacao;
        this.classificacaoRisco = risco;
        this.avaliadaEm = agora;
        this.avaliadaPorNome = enfermeiro.nome();
        this.atualizadaEm = agora;
        this.atualizadaPorNome = enfermeiro.nome();
    }

    public void mudarStatus(StatusTriagem novo, Profissional quem, LocalDateTime agora) {
        this.status = novo;
        this.atualizadaEm = agora;
        this.atualizadaPorNome = quem.nome();
    }

    public Integer getId() { return id; }
    public Integer getPacienteId() { return pacienteId; }
    public String getPacienteNome() { return pacienteNome; }
    public String getQueixaPrincipal() { return queixaPrincipal; }
    public String getSintomas() { return sintomas; }
    public String getPressaoArterial() { return pressaoArterial; }
    public BigDecimal getTemperatura() { return temperatura; }
    public Integer getFrequenciaCardiaca() { return frequenciaCardiaca; }
    public Integer getSaturacao() { return saturacao; }
    public ClassificacaoRisco getClassificacaoRisco() { return classificacaoRisco; }
    public StatusTriagem getStatus() { return status; }
    public LocalDateTime getAbertaEm() { return abertaEm; }
    public String getAbertaPorNome() { return abertaPorNome; }
    public String getAbertaPorPerfil() { return abertaPorPerfil; }
    public LocalDateTime getAvaliadaEm() { return avaliadaEm; }
    public String getAvaliadaPorNome() { return avaliadaPorNome; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
    public String getAtualizadaPorNome() { return atualizadaPorNome; }
    public Long getVersao() { return versao; }
}
