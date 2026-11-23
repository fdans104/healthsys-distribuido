package com.healthsys.record.eventos;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Caixa de saída (padrão "outbox"), guardada no próprio MongoDB do prontuário.
 * O evento é gravado logo depois do registro clínico e o PublicadorDeEventos envia para o Kafka.
 * Se o Kafka estiver fora do ar, o evento fica aqui esperando e é enviado quando ele voltar.
 */
@Document("eventos_pendentes")
public class EventoPendente {

    /** O próprio eventoId: quem consome usa para não processar o mesmo evento duas vezes. */
    @Id
    private String id;

    private String topico;

    /** Chave da mensagem no Kafka (id do paciente): eventos do mesmo paciente chegam na ordem. */
    private String chave;

    private String payload;

    @Indexed
    private Instant criadoEm;

    @Indexed
    private Instant enviadoEm;

    private int tentativas;

    private String ultimoErro;

    protected EventoPendente() {
    }

    public EventoPendente(String eventoId, String topico, String chave, String payload, Instant criadoEm) {
        this.id = eventoId;
        this.topico = topico;
        this.chave = chave;
        this.payload = payload;
        this.criadoEm = criadoEm;
    }

    public void marcarEnviado(Instant quando) {
        this.enviadoEm = quando;
        this.ultimoErro = null;
    }

    public void registrarFalha(String erro) {
        this.tentativas++;
        if (erro != null && erro.length() > 500) {
            erro = erro.substring(0, 500);
        }
        this.ultimoErro = erro;
    }

    public String getEventoId() { return id; }
    public String getTopico() { return topico; }
    public String getChave() { return chave; }
    public String getPayload() { return payload; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getEnviadoEm() { return enviadoEm; }
    public int getTentativas() { return tentativas; }
    public String getUltimoErro() { return ultimoErro; }
}
