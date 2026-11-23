package com.healthsys.monitoring.sinais;

import com.healthsys.monitoring.eventos.RegistroDeEventos;
import com.healthsys.monitoring.exception.RecursoNaoEncontradoException;
import com.healthsys.monitoring.exception.RegraNegocioException;
import com.healthsys.monitoring.leito.Leito;
import com.healthsys.monitoring.leito.LeitoRepository;
import com.healthsys.monitoring.leito.Profissional;
import com.healthsys.monitoring.leito.StatusLeito;
import com.healthsys.monitoring.sinais.dto.AlertaResponse;
import com.healthsys.monitoring.sinais.dto.MonitorLeitoResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Recebe as leituras dos monitores (via MQTT), mostra os sinais vitais ao vivo e gera alertas.
 * Cada alerta novo vira um evento ALERTA_SINAIS_VITAIS no Kafka; o notification-service avisa médicos e enfermagem.
 */
@Service
public class MonitoramentoService {

    private final LeitoRepository leitos;
    private final AlertaRepository alertas;
    private final PainelDeLeituras painel;
    private final RegistroDeEventos eventos;
    private final MeterRegistry metricas;
    private final Clock relogio;

    @Autowired
    public MonitoramentoService(LeitoRepository leitos, AlertaRepository alertas, PainelDeLeituras painel,
                                RegistroDeEventos eventos, MeterRegistry metricas) {
        this(leitos, alertas, painel, eventos, metricas, Clock.systemDefaultZone());
    }

    MonitoramentoService(LeitoRepository leitos, AlertaRepository alertas, PainelDeLeituras painel,
                         RegistroDeEventos eventos, MeterRegistry metricas, Clock relogio) {
        this.leitos = leitos;
        this.alertas = alertas;
        this.painel = painel;
        this.eventos = eventos;
        this.metricas = metricas;
        this.relogio = relogio;
    }

    /**
     * Uma leitura chegou do monitor do leito. Leitos sem paciente são ignorados (o monitor está ligado, mas
     * não há a quem avisar). Devolve os alertas novos que foram criados.
     */
    @Transactional
    public List<Alerta> registrarLeitura(String codigoLeito, LeituraSinais leitura) {
        if (leitura == null || leitura.vazia()) {
            return List.of();
        }
        Optional<Leito> encontrado = leitos.findByCodigo(codigoLeito);
        if (encontrado.isEmpty() || encontrado.get().getStatus() != StatusLeito.OCUPADO) {
            painel.descartar(codigoLeito);
            return List.of();
        }
        Leito leito = encontrado.get();
        painel.registrar(codigoLeito, leitura, Instant.now(relogio));

        LocalDateTime agora = LocalDateTime.now(relogio);
        List<Alerta> novos = new java.util.ArrayList<>();
        for (Achado achado : AvaliadorSinaisVitais.avaliar(leitura)) {
            Alerta ultimo = alertas.findFirstByLeitoCodigoAndTipoOrderByCriadoEmDesc(codigoLeito, achado.tipo()).orElse(null);
            if (!RegraDeRepeticao.deveAlertar(ultimo, leito.getPacienteId(), achado.gravidade(), agora)) {
                continue;
            }
            Alerta alerta = alertas.save(new Alerta(codigoLeito, leito.getSetor().name(), leito.getPacienteId(),
                    leito.getPacienteNome(), achado, agora));
            novos.add(alerta);
            Counter.builder("healthsys.alertas")
                    .description("Alertas de sinais vitais gerados pelos monitores dos leitos")
                    .tag("gravidade", achado.gravidade().name())
                    .tag("tipo", achado.tipo().name())
                    .register(metricas)
                    .increment();

            Map<String, Object> dados = new LinkedHashMap<>();
            dados.put("alertaId", alerta.getId());
            dados.put("leito", codigoLeito);
            dados.put("setor", leito.getSetor().name());
            dados.put("tipo", achado.tipo().name());
            dados.put("gravidade", achado.gravidade().name());
            dados.put("valor", achado.valor());
            dados.put("mensagem", achado.mensagem());
            eventos.registrar("ALERTA_SINAIS_VITAIS", leito.getPacienteId(), leito.getPacienteNome(), null, dados);
        }
        return novos;
    }

    /** Leitos ocupados com os sinais vitais mais recentes. */
    @Transactional(readOnly = true)
    public List<MonitorLeitoResponse> leitosMonitorados() {
        Instant agora = Instant.now(relogio);
        return leitos.findByStatusOrderBySetorAscCodigoAsc(StatusLeito.OCUPADO).stream()
                .map(l -> {
                    LeituraRecebida recebida = painel.ultima(l.getCodigo()).orElse(null);
                    LeituraSinais leitura = recebida == null ? null : recebida.leitura();
                    return new MonitorLeitoResponse(l.getId(), l.getCodigo(), l.getSetor(), l.getPacienteId(),
                            l.getPacienteNome(), l.getOcupadoEm(), leitura,
                            recebida == null ? null : recebida.recebidaEm(),
                            PainelDeLeituras.semSinal(recebida, agora), estados(leitura),
                            alertas.countByLeitoCodigoAndReconhecidoEmIsNull(l.getCodigo()));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertaResponse> alertas(boolean somenteAtivos) {
        List<Alerta> lista = somenteAtivos
                ? alertas.findByReconhecidoEmIsNullOrderByCriadoEmDesc()
                : alertas.findTop100ByOrderByCriadoEmDesc();
        return lista.stream().map(AlertaResponse::de).toList();
    }

    /** O médico ou enfermeiro confirma que viu o alerta e está cuidando do paciente. */
    @Transactional
    public AlertaResponse reconhecer(Integer id, Profissional quem) {
        Alerta alerta = alertas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta " + id + " não encontrado"));
        if (!alerta.ativo()) {
            throw new RegraNegocioException("Este alerta já foi reconhecido por " + alerta.getReconhecidoPorNome() + ".");
        }
        alerta.reconhecer(quem.nome(), LocalDateTime.now(relogio));
        return AlertaResponse.de(alertas.save(alerta));
    }

    /** Situação de cada sinal da leitura: NORMAL, ATENCAO ou CRITICO (o pior achado de cada sinal). */
    static Map<String, String> estados(LeituraSinais leitura) {
        Map<String, String> estados = new LinkedHashMap<>();
        if (leitura == null) {
            return estados;
        }
        if (leitura.saturacao() != null) estados.put("saturacao", "NORMAL");
        if (leitura.frequenciaCardiaca() != null) estados.put("frequenciaCardiaca", "NORMAL");
        if (leitura.temperatura() != null) estados.put("temperatura", "NORMAL");
        if (leitura.pressaoSistolica() != null) estados.put("pressao", "NORMAL");
        for (Achado achado : AvaliadorSinaisVitais.avaliar(leitura)) {
            String sinal = achado.tipo().sinal();
            if (!"CRITICO".equals(estados.get(sinal))) {
                estados.put(sinal, achado.gravidade().name());
            }
        }
        return estados;
    }
}
