package com.healthsys.record.prontuario;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** RF11: consulta médica registrada no prontuário. Não é editada nem apagada. */
@Document("consultas")
public class Consulta {

    @Id
    private String id;

    @Indexed
    private Integer pacienteId;

    private LocalDateTime data;
    private String motivo;
    private String avaliacao;
    private String diagnostico;
    private String conduta;

    private LocalDateTime registradoEm;
    private Integer registradoPorId;
    private String registradoPorNome;
    private String registradoPorPerfil;

    protected Consulta() {
        // exigido pelo Spring Data
    }

    public Consulta(Integer pacienteId, LocalDateTime data, String motivo, String avaliacao, String diagnostico,
                    String conduta, Profissional medico, LocalDateTime agora) {
        this.pacienteId = pacienteId;
        this.data = data;
        this.motivo = motivo;
        this.avaliacao = avaliacao;
        this.diagnostico = diagnostico;
        this.conduta = conduta;
        this.registradoEm = agora;
        this.registradoPorId = medico.id();
        this.registradoPorNome = medico.nome();
        this.registradoPorPerfil = medico.perfil();
    }

    public String getId() { return id; }
    public Integer getPacienteId() { return pacienteId; }
    public LocalDateTime getData() { return data; }
    public String getMotivo() { return motivo; }
    public String getAvaliacao() { return avaliacao; }
    public String getDiagnostico() { return diagnostico; }
    public String getConduta() { return conduta; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public Integer getRegistradoPorId() { return registradoPorId; }
    public String getRegistradoPorNome() { return registradoPorNome; }
    public String getRegistradoPorPerfil() { return registradoPorPerfil; }
}
