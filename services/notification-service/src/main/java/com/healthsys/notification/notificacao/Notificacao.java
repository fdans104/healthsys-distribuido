package com.healthsys.notification.notificacao;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Um aviso para um perfil (ex.: todos os médicos) ou para uma pessoa (ex.: o médico que pediu o exame).
 * O id é o próprio eventoId: se o Kafka entregar o mesmo evento duas vezes, a notificação não se repete.
 * Notificações com mais de 30 dias são apagadas automaticamente pelo MongoDB (índice TTL).
 */
@Document("notificacoes")
public class Notificacao {

    @Id
    private String id;

    private String tipo;
    private String titulo;
    private String mensagem;
    private Gravidade gravidade;

    /** Perfis que recebem (usado quando não há um destinatário específico). */
    @Indexed
    private List<String> perfisDestino;

    /** Quando preenchido, só essa pessoa recebe. */
    @Indexed
    private Integer usuarioDestinoId;

    private Integer pacienteId;
    private String pacienteNome;

    /** Seção da tela para onde o aviso leva: triagem, pacientes ou leitos. */
    private String secao;

    private String origem;
    private Instant ocorridoEm;

    @Indexed(expireAfterSeconds = 2592000)
    private Instant criadaEm;

    /** Ids dos usuários que já leram. */
    private Set<Integer> lidaPor = new HashSet<>();

    protected Notificacao() {
    }

    public Notificacao(String id, String tipo, String titulo, String mensagem, Gravidade gravidade,
                       List<String> perfisDestino, Integer usuarioDestinoId, Integer pacienteId, String pacienteNome,
                       String secao, String origem, Instant ocorridoEm, Instant criadaEm) {
        this.id = id;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.gravidade = gravidade;
        this.perfisDestino = perfisDestino == null ? List.of() : List.copyOf(perfisDestino);
        this.usuarioDestinoId = usuarioDestinoId;
        this.pacienteId = pacienteId;
        this.pacienteNome = pacienteNome;
        this.secao = secao;
        this.origem = origem;
        this.ocorridoEm = ocorridoEm;
        this.criadaEm = criadaEm;
    }

    /** Esta notificação é para essa pessoa? */
    public boolean destinadaA(Integer usuarioId, String perfil) {
        if (usuarioDestinoId != null) {
            return usuarioDestinoId.equals(usuarioId);
        }
        return perfil != null && perfisDestino != null && perfisDestino.contains(perfil);
    }

    public boolean lidaPor(Integer usuarioId) {
        return usuarioId != null && lidaPor != null && lidaPor.contains(usuarioId);
    }

    public String getId() { return id; }
    public String getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public String getMensagem() { return mensagem; }
    public Gravidade getGravidade() { return gravidade; }
    public List<String> getPerfisDestino() { return perfisDestino; }
    public Integer getUsuarioDestinoId() { return usuarioDestinoId; }
    public Integer getPacienteId() { return pacienteId; }
    public String getPacienteNome() { return pacienteNome; }
    public String getSecao() { return secao; }
    public String getOrigem() { return origem; }
    public Instant getOcorridoEm() { return ocorridoEm; }
    public Instant getCriadaEm() { return criadaEm; }
    public Set<Integer> getLidaPor() { return lidaPor; }
}
