package com.healthsys.record.prontuario;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * RF13: medicamento prescrito pelo médico. Não é apagado: quando deixa de ser usado, o médico o suspende,
 * informando o motivo, e a prescrição continua no prontuário.
 */
@Document("medicamentos")
public class Medicamento {

    @Id
    private String id;

    @Indexed
    private Integer pacienteId;

    /** Nome do paciente no momento do registro (usado nas notificações). */
    private String pacienteNome;

    private String nome;
    private String dosagem;
    private ViaAdministracao via;
    private String frequencia;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String observacoes;
    private StatusMedicamento status;

    private LocalDateTime prescritoEm;
    private Integer prescritoPorId;
    private String prescritoPorNome;
    private String prescritoPorPerfil;

    private String motivoSuspensao;
    private LocalDateTime suspensoEm;
    private String suspensoPorNome;

    protected Medicamento() {
        // exigido pelo Spring Data
    }

    public Medicamento(Integer pacienteId, String nome, String dosagem, ViaAdministracao via, String frequencia,
                       LocalDate dataInicio, LocalDate dataFim, String observacoes, Profissional medico,
                       LocalDateTime agora) {
        this.pacienteId = pacienteId;
        this.nome = nome;
        this.dosagem = dosagem;
        this.via = via;
        this.frequencia = frequencia;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.observacoes = observacoes;
        this.status = StatusMedicamento.ATIVO;
        this.prescritoEm = agora;
        this.prescritoPorId = medico.id();
        this.prescritoPorNome = medico.nome();
        this.prescritoPorPerfil = medico.perfil();
    }

    public void suspender(String motivo, Profissional medico, LocalDateTime agora) {
        this.status = StatusMedicamento.SUSPENSO;
        this.motivoSuspensao = motivo;
        this.suspensoEm = agora;
        this.suspensoPorNome = medico.nome();
    }

    /** Guarda o nome informado pelo patient-service, para os avisos não precisarem consultá-lo de novo. */
    public void registrarNomeDoPaciente(String nome) {
        this.pacienteNome = nome;
    }

    public String getId() { return id; }
    public Integer getPacienteId() { return pacienteId; }
    public String getPacienteNome() { return pacienteNome; }
    public String getNome() { return nome; }
    public String getDosagem() { return dosagem; }
    public ViaAdministracao getVia() { return via; }
    public String getFrequencia() { return frequencia; }
    public LocalDate getDataInicio() { return dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public String getObservacoes() { return observacoes; }
    public StatusMedicamento getStatus() { return status; }
    public LocalDateTime getPrescritoEm() { return prescritoEm; }
    public Integer getPrescritoPorId() { return prescritoPorId; }
    public String getPrescritoPorNome() { return prescritoPorNome; }
    public String getPrescritoPorPerfil() { return prescritoPorPerfil; }
    public String getMotivoSuspensao() { return motivoSuspensao; }
    public LocalDateTime getSuspensoEm() { return suspensoEm; }
    public String getSuspensoPorNome() { return suspensoPorNome; }
}
