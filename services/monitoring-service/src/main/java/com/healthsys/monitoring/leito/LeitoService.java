package com.healthsys.monitoring.leito;

import com.healthsys.monitoring.eventos.RegistroDeEventos;
import com.healthsys.monitoring.exception.ConflitoEdicaoException;
import com.healthsys.monitoring.exception.RecursoNaoEncontradoException;
import com.healthsys.monitoring.exception.RegraNegocioException;
import com.healthsys.monitoring.leito.dto.LeitoRequest;
import com.healthsys.monitoring.leito.dto.LeitoResponse;
import com.healthsys.monitoring.leito.dto.OcupacaoRequest;
import com.healthsys.monitoring.leito.dto.OcupacaoSetorResponse;
import com.healthsys.monitoring.paciente.PacienteClient;
import com.healthsys.monitoring.paciente.PacienteResumo;
import com.healthsys.monitoring.sinais.AlertaRepository;
import com.healthsys.monitoring.sinais.PainelDeLeituras;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gestão de leitos (logística hospitalar):
 * LIVRE → OCUPADO (internação) → HIGIENIZACAO (alta) → LIVRE (a enfermagem confirma a limpeza).
 * - Um leito só recebe paciente se estiver livre; um paciente só ocupa um leito por vez.
 * - RN54: quem age com uma versão antiga do leito é recusado (duas pessoas não internam no mesmo leito).
 * - Cada mudança gera um evento (LEITO_OCUPADO, LEITO_LIBERADO, LEITO_HIGIENIZADO) para as notificações.
 */
@Service
public class LeitoService {

    private final LeitoRepository leitos;
    private final AlertaRepository alertas;
    private final PainelDeLeituras painel;
    private final PacienteClient pacientes;
    private final RegistroDeEventos eventos;
    private final Clock relogio;

    @Autowired
    public LeitoService(LeitoRepository leitos, AlertaRepository alertas, PainelDeLeituras painel,
                        PacienteClient pacientes, RegistroDeEventos eventos) {
        this(leitos, alertas, painel, pacientes, eventos, Clock.systemDefaultZone());
    }

    LeitoService(LeitoRepository leitos, AlertaRepository alertas, PainelDeLeituras painel, PacienteClient pacientes,
                 RegistroDeEventos eventos, Clock relogio) {
        this.leitos = leitos;
        this.alertas = alertas;
        this.painel = painel;
        this.pacientes = pacientes;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<LeitoResponse> listar() {
        return leitos.findAllByOrderBySetorAscCodigoAsc().stream().map(LeitoResponse::de).toList();
    }

    /** Painel de ocupação por setor. */
    @Transactional(readOnly = true)
    public List<OcupacaoSetorResponse> ocupacao() {
        return Arrays.stream(Setor.values())
                .map(setor -> OcupacaoSetorResponse.de(setor,
                        leitos.countBySetorAndStatus(setor, StatusLeito.LIVRE),
                        leitos.countBySetorAndStatus(setor, StatusLeito.OCUPADO),
                        leitos.countBySetorAndStatus(setor, StatusLeito.HIGIENIZACAO)))
                .toList();
    }

    @Transactional
    public LeitoResponse criar(LeitoRequest req, Profissional admin) {
        String codigo = req.codigo().trim().toUpperCase(Locale.ROOT);
        if (leitos.existsByCodigo(codigo)) {
            throw new RegraNegocioException("Já existe um leito com o código " + codigo + ".");
        }
        return LeitoResponse.de(leitos.save(new Leito(codigo, req.setor(), admin, agora())));
    }

    @Transactional
    public void remover(Integer id) {
        Leito leito = buscar(id);
        if (leito.getStatus() != StatusLeito.LIVRE) {
            throw new RegraNegocioException("Só é possível remover um leito livre.");
        }
        painel.descartar(leito.getCodigo());
        leitos.delete(leito);
    }

    /** Internação: o paciente (confirmado no patient-service) passa a ocupar o leito. */
    @Transactional
    public LeitoResponse ocupar(Integer id, OcupacaoRequest req, Profissional quem, String authorization) {
        Leito leito = buscar(id);
        exigirVersaoAtual(leito, req.versao());
        if (leito.getStatus() == StatusLeito.OCUPADO) {
            throw new RegraNegocioException("O leito " + leito.getCodigo() + " já está ocupado.");
        }
        if (leito.getStatus() == StatusLeito.HIGIENIZACAO) {
            throw new RegraNegocioException("O leito " + leito.getCodigo() + " ainda está em higienização.");
        }
        leitos.findByPacienteId(req.pacienteId()).ifPresent(outro -> {
            throw new RegraNegocioException("Este paciente já está internado no leito " + outro.getCodigo() + ".");
        });
        PacienteResumo paciente = pacientes.buscar(req.pacienteId(), authorization);

        leito.ocupar(paciente.id(), paciente.nome(), quem, agora());
        Leito salvo = leitos.saveAndFlush(leito);
        // Leituras antigas (de antes da internação) não valem para este paciente.
        painel.descartar(salvo.getCodigo());
        eventos.registrar("LEITO_OCUPADO", salvo.getPacienteId(), salvo.getPacienteNome(), quem, dados(salvo));
        return LeitoResponse.de(salvo);
    }

    /** Alta do leito: o paciente sai, os alertas abertos são encerrados e o leito vai para a higienização. */
    @Transactional
    public LeitoResponse liberar(Integer id, Long versao, Profissional quem) {
        Leito leito = buscar(id);
        exigirVersaoAtual(leito, versao);
        if (leito.getStatus() != StatusLeito.OCUPADO) {
            throw new RegraNegocioException("O leito " + leito.getCodigo() + " não está ocupado.");
        }
        Integer pacienteId = leito.getPacienteId();
        String pacienteNome = leito.getPacienteNome();

        LocalDateTime agora = agora();
        alertas.encerrarAtivosDoLeito(leito.getCodigo(), agora, "Encerrado na alta do leito");
        leito.liberar(quem, agora);
        Leito salvo = leitos.saveAndFlush(leito);
        painel.descartar(salvo.getCodigo());
        eventos.registrar("LEITO_LIBERADO", pacienteId, pacienteNome, quem, dados(salvo));
        return LeitoResponse.de(salvo);
    }

    @Transactional
    public LeitoResponse marcarHigienizado(Integer id, Long versao, Profissional quem) {
        Leito leito = buscar(id);
        exigirVersaoAtual(leito, versao);
        if (leito.getStatus() != StatusLeito.HIGIENIZACAO) {
            throw new RegraNegocioException("O leito " + leito.getCodigo() + " não está em higienização.");
        }
        leito.marcarHigienizado(quem, agora());
        Leito salvo = leitos.saveAndFlush(leito);
        eventos.registrar("LEITO_HIGIENIZADO", null, null, quem, dados(salvo));
        return LeitoResponse.de(salvo);
    }

    private Leito buscar(Integer id) {
        return leitos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Leito " + id + " não encontrado"));
    }

    private static void exigirVersaoAtual(Leito leito, Long versaoEnviada) {
        if (versaoEnviada == null || !versaoEnviada.equals(leito.getVersao())) {
            throw new ConflitoEdicaoException();
        }
    }

    private static Map<String, Object> dados(Leito l) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("leitoId", l.getId());
        dados.put("leito", l.getCodigo());
        dados.put("setor", l.getSetor().name());
        dados.put("status", l.getStatus().name());
        return dados;
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }
}
