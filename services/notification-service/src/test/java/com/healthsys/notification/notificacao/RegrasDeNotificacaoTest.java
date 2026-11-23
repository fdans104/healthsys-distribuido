package com.healthsys.notification.notificacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegrasDeNotificacaoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Instant AGORA = Instant.parse("2026-11-22T10:00:00Z");

    private static JsonNode evento(String tipo, String dados) throws Exception {
        return MAPPER.readTree("""
                {"eventoId":"ev-1","tipo":"%s","origem":"teste","ocorridoEm":"2026-11-22T09:59:00Z",
                 "pacienteId":1,"pacienteNome":"Maria Silva",
                 "autor":{"id":7,"nome":"Dra. Ana","perfil":"MEDICO"},
                 "dados":%s}
                """.formatted(tipo, dados));
    }

    @Test
    void pacienteNaTriagemAvisaAEnfermagem() throws Exception {
        Notificacao n = RegrasDeNotificacao.traduzir(
                evento("TRIAGEM_ABERTA", "{\"queixaPrincipal\":\"Febre\"}"), AGORA).orElseThrow();

        assertThat(n.getId()).isEqualTo("ev-1");
        assertThat(n.getPerfisDestino()).containsExactly("ENFERMEIRO");
        assertThat(n.getMensagem()).contains("Maria Silva").contains("Febre");
        assertThat(n.getGravidade()).isEqualTo(Gravidade.INFO);
        assertThat(n.getSecao()).isEqualTo("triagem");
        assertThat(n.getOcorridoEm()).isEqualTo(Instant.parse("2026-11-22T09:59:00Z"));
        assertThat(n.getCriadaEm()).isEqualTo(AGORA);
    }

    @Test
    void riscoCriticoAvisaMedicoEEnfermagemEmDestaque() throws Exception {
        Notificacao n = RegrasDeNotificacao.traduzir(
                evento("TRIAGEM_AVALIADA", "{\"classificacaoRisco\":\"CRITICO\"}"), AGORA).orElseThrow();

        assertThat(n.getGravidade()).isEqualTo(Gravidade.CRITICO);
        assertThat(n.getPerfisDestino()).containsExactlyInAnyOrder("MEDICO", "ENFERMEIRO");
        assertThat(n.destinadaA(99, "MEDICO")).isTrue();
        assertThat(n.destinadaA(99, "RECEPCIONISTA")).isFalse();
        assertThat(n.destinadaA(99, "ADMIN")).isFalse();
    }

    @Test
    void riscoMedioAvisaSoOMedico() throws Exception {
        Notificacao n = RegrasDeNotificacao.traduzir(
                evento("TRIAGEM_AVALIADA", "{\"classificacaoRisco\":\"MEDIO\"}"), AGORA).orElseThrow();

        assertThat(n.getGravidade()).isEqualTo(Gravidade.INFO);
        assertThat(n.getPerfisDestino()).containsExactly("MEDICO");
        assertThat(n.getMensagem()).contains("médio");
    }

    @Test
    void pacienteChamadoAvisaARecepcao() throws Exception {
        Notificacao n = RegrasDeNotificacao.traduzir(
                evento("TRIAGEM_STATUS_ALTERADO", "{\"status\":\"EM_ATENDIMENTO\"}"), AGORA).orElseThrow();

        assertThat(n.getPerfisDestino()).containsExactly("RECEPCIONISTA");
        assertThat(n.getMensagem()).contains("Dra. Ana");
    }

    @Test
    void resultadoDeExameVaiSoParaOMedicoQuePediu() throws Exception {
        Notificacao n = RegrasDeNotificacao.traduzir(evento("EXAME_RESULTADO_REGISTRADO",
                "{\"nome\":\"Hemograma\",\"solicitadoPorId\":7}"), AGORA).orElseThrow();

        assertThat(n.getUsuarioDestinoId()).isEqualTo(7);
        assertThat(n.destinadaA(7, "MEDICO")).isTrue();
        assertThat(n.destinadaA(8, "MEDICO")).isFalse();
        assertThat(n.getGravidade()).isEqualTo(Gravidade.ATENCAO);
    }

    @Test
    void prescricaoAvisaAEnfermagemComDoseEFrequencia() throws Exception {
        Notificacao n = RegrasDeNotificacao.traduzir(evento("MEDICAMENTO_PRESCRITO",
                "{\"nome\":\"Dipirona\",\"dosagem\":\"500 mg\",\"frequencia\":\"de 6 em 6 horas\"}"), AGORA).orElseThrow();

        assertThat(n.getPerfisDestino()).containsExactly("ENFERMEIRO");
        assertThat(n.getMensagem()).isEqualTo("Dra. Ana prescreveu Dipirona 500 mg, de 6 em 6 horas para Maria Silva.");
    }

    @Test
    void alertaCriticoDoMonitorAvisaMedicoEEnfermagem() throws Exception {
        Notificacao n = RegrasDeNotificacao.traduzir(evento("ALERTA_SINAIS_VITAIS",
                "{\"leito\":\"UTI-01\",\"gravidade\":\"CRITICO\",\"mensagem\":\"saturação 84%\"}"), AGORA).orElseThrow();

        assertThat(n.getGravidade()).isEqualTo(Gravidade.CRITICO);
        assertThat(n.getTitulo()).contains("UTI-01");
        assertThat(n.getPerfisDestino()).containsExactlyInAnyOrder("MEDICO", "ENFERMEIRO");
        assertThat(n.getSecao()).isEqualTo("leitos");
    }

    @Test
    void consultaRegistradaNaoGeraAviso() throws Exception {
        Optional<Notificacao> n = RegrasDeNotificacao.traduzir(evento("CONSULTA_REGISTRADA", "{}"), AGORA);
        assertThat(n).isEmpty();
    }

    @Test
    void semAutorNemNomeDoPacienteOTextoContinuaClaro() throws Exception {
        JsonNode e = MAPPER.readTree("""
                {"eventoId":"ev-2","tipo":"LEITO_LIBERADO","pacienteId":5,"dados":{"leito":"ENF-02"}}
                """);
        Notificacao n = RegrasDeNotificacao.traduzir(e, AGORA).orElseThrow();

        assertThat(n.getMensagem()).isEqualTo("O leito ENF-02 foi liberado por um profissional e precisa de higienização.");
        assertThat(n.getPacienteNome()).isNull();
    }

    @Test
    void eventoForaDoFormatoEhRecusado() throws Exception {
        assertThatThrownBy(() -> RegrasDeNotificacao.traduzir(MAPPER.readTree("{\"tipo\":\"X\"}"), AGORA))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
