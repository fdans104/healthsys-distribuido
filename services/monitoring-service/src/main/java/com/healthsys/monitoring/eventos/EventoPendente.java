package com.healthsys.monitoring.eventos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Caixa de saída (padrão "transactional outbox").
 * O evento é gravado no MESMO banco e na MESMA transação da mudança que o gerou.
 * Depois o PublicadorDeEventos envia para o Kafka. Se o Kafka estiver fora do ar,
 * o evento fica aqui esperando e é enviado quando ele voltar: nenhum aviso se perde.
 */
@Entity
@Table(name = "eventos_pendentes")
public class EventoPendente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long id;

    /** Identificador único do evento: quem consome usa para não processar o mesmo evento duas vezes. */
    @Column(name = "evento_id", nullable = false, unique = true, length = 36)
    private String eventoId;

    @Column(nullable = false, length = 100)
    private String topico;

    /** Chave da mensagem no Kafka (id do paciente): eventos do mesmo paciente chegam na ordem. */
    @Column(length = 100)
    private String chave;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "ultimo_erro", length = 500)
    private String ultimoErro;

    protected EventoPendente() {
    }

    public EventoPendente(String eventoId, String topico, String chave, String payload, Instant criadoEm) {
        this.eventoId = eventoId;
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

    public Long getId() { return id; }
    public String getEventoId() { return eventoId; }
    public String getTopico() { return topico; }
    public String getChave() { return chave; }
    public String getPayload() { return payload; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getEnviadoEm() { return enviadoEm; }
    public int getTentativas() { return tentativas; }
    public String getUltimoErro() { return ultimoErro; }
}
