package com.healthsys.notification.notificacao;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Traduz cada evento recebido do Kafka em um aviso para quem precisa agir, como num hospital:
 * - Chegou paciente na triagem: avisa a ENFERMAGEM.
 * - Enfermagem classificou o risco: avisa o MÉDICO (risco crítico também avisa a enfermagem, em destaque).
 * - Médico chamou, finalizou ou cancelou: avisa a RECEPÇÃO.
 * - Exame pedido ou remédio prescrito: avisa a ENFERMAGEM. Resultado do exame: avisa o MÉDICO QUE PEDIU.
 * - Monitor do leito detectou sinal vital fora do normal: avisa MÉDICO e ENFERMAGEM.
 * Eventos que não precisam de aviso (ex.: consulta registrada) são ignorados.
 */
public final class RegrasDeNotificacao {

    private static final String MEDICO = "MEDICO";
    private static final String ENFERMEIRO = "ENFERMEIRO";
    private static final String RECEPCIONISTA = "RECEPCIONISTA";

    private RegrasDeNotificacao() {
    }

    public static Optional<Notificacao> traduzir(JsonNode evento, Instant agora) {
        String eventoId = texto(evento, "eventoId");
        String tipo = texto(evento, "tipo");
        if (eventoId == null || tipo == null) {
            throw new IllegalArgumentException("Evento sem eventoId ou tipo");
        }
        JsonNode dados = evento.path("dados");
        String paciente = nomeDoPaciente(evento);
        String autor = nomeDoAutor(evento);

        Aviso aviso = switch (tipo) {
            case "TRIAGEM_ABERTA" -> new Aviso("Novo paciente na triagem",
                    paciente + " chegou com a queixa: " + valorOu(texto(dados, "queixaPrincipal"), "não informada")
                            + ". Aguarda avaliação da enfermagem.",
                    Gravidade.INFO, List.of(ENFERMEIRO), null, "triagem");
            case "TRIAGEM_AVALIADA" -> triagemAvaliada(paciente, autor, texto(dados, "classificacaoRisco"));
            case "TRIAGEM_STATUS_ALTERADO" -> triagemMudouDeStatus(paciente, autor, texto(dados, "status"));
            case "EXAME_SOLICITADO" -> new Aviso("Exame solicitado",
                    autor + " pediu " + valorOu(texto(dados, "nome"), "um exame") + " para " + paciente + ".",
                    Gravidade.INFO, List.of(ENFERMEIRO), null, "pacientes");
            case "EXAME_RESULTADO_REGISTRADO" -> {
                Integer quemPediu = inteiro(dados, "solicitadoPorId");
                yield new Aviso("Resultado de exame disponível",
                        "O resultado de " + valorOu(texto(dados, "nome"), "um exame") + " de " + paciente
                                + " foi registrado por " + autor + ".",
                        Gravidade.ATENCAO, quemPediu == null ? List.of(MEDICO) : List.of(), quemPediu, "pacientes");
            }
            case "MEDICAMENTO_PRESCRITO" -> new Aviso("Nova prescrição",
                    autor + " prescreveu " + valorOu(texto(dados, "nome"), "um medicamento")
                            + juntar(" ", texto(dados, "dosagem")) + juntar(", ", texto(dados, "frequencia"))
                            + " para " + paciente + ".",
                    Gravidade.INFO, List.of(ENFERMEIRO), null, "pacientes");
            case "MEDICAMENTO_SUSPENSO" -> new Aviso("Prescrição suspensa",
                    valorOu(texto(dados, "nome"), "Um medicamento") + " de " + paciente + " foi suspenso por " + autor
                            + juntar(". Motivo: ", texto(dados, "motivoSuspensao")) + ".",
                    Gravidade.ATENCAO, List.of(ENFERMEIRO), null, "pacientes");
            case "ALERTA_SINAIS_VITAIS" -> alertaDoMonitor(paciente, dados);
            case "LEITO_OCUPADO" -> new Aviso("Paciente internado",
                    paciente + " foi internado no leito " + valorOu(texto(dados, "leito"), "?") + " por " + autor + ".",
                    Gravidade.INFO, List.of(RECEPCIONISTA), null, "leitos");
            case "LEITO_LIBERADO" -> new Aviso("Leito aguardando higienização",
                    "O leito " + valorOu(texto(dados, "leito"), "?") + " foi liberado por " + autor
                            + " e precisa de higienização.",
                    Gravidade.INFO, List.of(ENFERMEIRO), null, "leitos");
            case "LEITO_HIGIENIZADO" -> new Aviso("Leito disponível",
                    "O leito " + valorOu(texto(dados, "leito"), "?") + " está livre para um novo paciente.",
                    Gravidade.INFO, List.of(RECEPCIONISTA), null, "leitos");
            default -> null;
        };
        if (aviso == null) {
            return Optional.empty();
        }
        return Optional.of(new Notificacao(eventoId, tipo, aviso.titulo(), aviso.mensagem(), aviso.gravidade(),
                aviso.perfis(), aviso.usuarioId(), inteiro(evento, "pacienteId"), texto(evento, "pacienteNome"),
                aviso.secao(), texto(evento, "origem"), instante(texto(evento, "ocorridoEm")), agora));
    }

    private static Aviso triagemAvaliada(String paciente, String autor, String risco) {
        if ("CRITICO".equals(risco)) {
            return new Aviso("Risco CRÍTICO na fila",
                    paciente + " foi classificado como risco crítico por " + autor + ". Atendimento imediato.",
                    Gravidade.CRITICO, List.of(MEDICO, ENFERMEIRO), null, "triagem");
        }
        if ("ALTO".equals(risco)) {
            return new Aviso("Risco alto na fila",
                    paciente + " foi classificado como risco alto por " + autor + ".",
                    Gravidade.ATENCAO, List.of(MEDICO), null, "triagem");
        }
        return new Aviso("Paciente pronto para atendimento",
                paciente + " foi classificado como risco " + rotuloRisco(risco) + " por " + autor + ".",
                Gravidade.INFO, List.of(MEDICO), null, "triagem");
    }

    private static Aviso triagemMudouDeStatus(String paciente, String autor, String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "EM_ATENDIMENTO" -> new Aviso("Paciente chamado",
                    paciente + " foi chamado para atendimento por " + autor + ".",
                    Gravidade.INFO, List.of(RECEPCIONISTA), null, "triagem");
            case "PENDENTE" -> new Aviso("Paciente voltou à fila",
                    paciente + " foi devolvido à fila por " + autor + ".",
                    Gravidade.INFO, List.of(RECEPCIONISTA, ENFERMEIRO), null, "triagem");
            case "FINALIZADA" -> new Aviso("Atendimento finalizado",
                    "O atendimento de " + paciente + " foi finalizado por " + autor + ".",
                    Gravidade.INFO, List.of(RECEPCIONISTA), null, "triagem");
            case "CANCELADA" -> new Aviso("Triagem cancelada",
                    "A triagem de " + paciente + " foi cancelada por " + autor + ".",
                    Gravidade.INFO, List.of(RECEPCIONISTA, ENFERMEIRO, MEDICO), null, "triagem");
            default -> null;
        };
    }

    private static Aviso alertaDoMonitor(String paciente, JsonNode dados) {
        boolean critico = "CRITICO".equals(texto(dados, "gravidade"));
        String leito = valorOu(texto(dados, "leito"), "?");
        return new Aviso(critico ? "ALERTA CRÍTICO no leito " + leito : "Alerta no leito " + leito,
                paciente + ": " + valorOu(texto(dados, "mensagem"), "sinal vital fora do normal") + ".",
                critico ? Gravidade.CRITICO : Gravidade.ATENCAO, List.of(MEDICO, ENFERMEIRO), null, "leitos");
    }

    private static String nomeDoPaciente(JsonNode evento) {
        String nome = texto(evento, "pacienteNome");
        if (nome != null) {
            return nome;
        }
        Integer id = inteiro(evento, "pacienteId");
        return id == null ? "O paciente" : "Paciente nº " + id;
    }

    private static String nomeDoAutor(JsonNode evento) {
        return valorOu(texto(evento.path("autor"), "nome"), "um profissional");
    }

    private static String rotuloRisco(String risco) {
        if (risco == null) {
            return "não informado";
        }
        return switch (risco) {
            case "MEDIO" -> "médio";
            case "BAIXO" -> "baixo";
            default -> risco.toLowerCase();
        };
    }

    static String texto(JsonNode no, String campo) {
        if (no == null) {
            return null;
        }
        JsonNode valor = no.get(campo);
        if (valor == null || valor.isNull() || valor.isMissingNode()) {
            return null;
        }
        String texto = valor.asText();
        return texto.isBlank() ? null : texto;
    }

    static Integer inteiro(JsonNode no, String campo) {
        if (no == null) {
            return null;
        }
        JsonNode valor = no.get(campo);
        if (valor == null || !valor.canConvertToInt() || !valor.isNumber()) {
            return null;
        }
        return valor.intValue();
    }

    private static Instant instante(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return Instant.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String valorOu(String valor, String padrao) {
        return valor == null ? padrao : valor;
    }

    private static String juntar(String separador, String valor) {
        return valor == null ? "" : separador + valor;
    }

    private record Aviso(String titulo, String mensagem, Gravidade gravidade, List<String> perfis,
                         Integer usuarioId, String secao) {
    }
}
