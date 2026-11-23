package com.healthsys.record.prontuario;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RF12: exame do paciente. O médico solicita; depois o médico ou a enfermagem registra o resultado.
 * Com o resultado registrado, o exame fica concluído e não muda mais.
 */
@Document("exames")
public class Exame {

    @Id
    private String id;

    @Indexed
    private Integer pacienteId;

    /** Nome do paciente no momento do registro (usado nas notificações). */
    private String pacienteNome;

    private String nome;
    private TipoExame tipo;
    private String observacoes;
    private StatusExame status;

    private LocalDateTime solicitadoEm;
    private Integer solicitadoPorId;
    private String solicitadoPorNome;
    private String solicitadoPorPerfil;

    private String resultado;
    private LocalDate dataResultado;
    private LocalDateTime resultadoRegistradoEm;
    private String resultadoRegistradoPorNome;
    private String resultadoRegistradoPorPerfil;

    protected Exame() {
        // exigido pelo Spring Data
    }

    public Exame(Integer pacienteId, String nome, TipoExame tipo, String observacoes, Profissional medico,
                 LocalDateTime agora) {
        this.pacienteId = pacienteId;
        this.nome = nome;
        this.tipo = tipo;
        this.observacoes = observacoes;
        this.status = StatusExame.SOLICITADO;
        this.solicitadoEm = agora;
        this.solicitadoPorId = medico.id();
        this.solicitadoPorNome = medico.nome();
        this.solicitadoPorPerfil = medico.perfil();
    }

    /** Registra o resultado e conclui o exame. */
    public void registrarResultado(String resultado, LocalDate dataResultado, Profissional profissional,
                                   LocalDateTime agora) {
        this.resultado = resultado;
        this.dataResultado = dataResultado;
        this.resultadoRegistradoEm = agora;
        this.resultadoRegistradoPorNome = profissional.nome();
        this.resultadoRegistradoPorPerfil = profissional.perfil();
        this.status = StatusExame.CONCLUIDO;
    }

    /** Guarda o nome informado pelo patient-service, para os avisos não precisarem consultá-lo de novo. */
    public void registrarNomeDoPaciente(String nome) {
        this.pacienteNome = nome;
    }

    public String getId() { return id; }
    public Integer getPacienteId() { return pacienteId; }
    public String getPacienteNome() { return pacienteNome; }
    public String getNome() { return nome; }
    public TipoExame getTipo() { return tipo; }
    public String getObservacoes() { return observacoes; }
    public StatusExame getStatus() { return status; }
    public LocalDateTime getSolicitadoEm() { return solicitadoEm; }
    public Integer getSolicitadoPorId() { return solicitadoPorId; }
    public String getSolicitadoPorNome() { return solicitadoPorNome; }
    public String getSolicitadoPorPerfil() { return solicitadoPorPerfil; }
    public String getResultado() { return resultado; }
    public LocalDate getDataResultado() { return dataResultado; }
    public LocalDateTime getResultadoRegistradoEm() { return resultadoRegistradoEm; }
    public String getResultadoRegistradoPorNome() { return resultadoRegistradoPorNome; }
    public String getResultadoRegistradoPorPerfil() { return resultadoRegistradoPorPerfil; }
}
