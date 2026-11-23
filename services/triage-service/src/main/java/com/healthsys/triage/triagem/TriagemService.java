package com.healthsys.triage.triagem;

import com.healthsys.triage.eventos.RegistroDeEventos;
import com.healthsys.triage.exception.AcaoNaoPermitidaException;
import com.healthsys.triage.exception.ConflitoEdicaoException;
import com.healthsys.triage.exception.RecursoNaoEncontradoException;
import com.healthsys.triage.exception.RegraNegocioException;
import com.healthsys.triage.paciente.PacienteClient;
import com.healthsys.triage.paciente.PacienteResumo;
import com.healthsys.triage.triagem.dto.AberturaRequest;
import com.healthsys.triage.triagem.dto.AvaliacaoRequest;
import com.healthsys.triage.triagem.dto.StatusRequest;
import com.healthsys.triage.triagem.dto.TriagemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regras da teletriagem (RF14 a RF16):
 * - RN34: toda triagem é de um paciente que existe (confirmado no patient-service).
 * - RN38/RN39: risco só BAIXO, MEDIO, ALTO ou CRITICO; sem risco não há atendimento nem finalização.
 * - RN40: status só PENDENTE, EM_ATENDIMENTO, FINALIZADA ou CANCELADA; triagem encerrada não volta atrás.
 * - RN41: quem pode cada mudança de status (o médico atende e finaliza; quem estiver na triagem pode cancelar).
 * - RN54: quem salva com uma versão antiga é recusado.
 * Cada mudança gera um evento (TRIAGEM_ABERTA, TRIAGEM_AVALIADA, TRIAGEM_STATUS_ALTERADO), gravado na mesma
 * transação e enviado depois ao Kafka. O notification-service avisa a enfermagem, o médico e a recepção.
 */
@Service
public class TriagemService {

    /** Mudanças de status permitidas. FINALIZADA e CANCELADA não têm saída (RN40). */
    private static final Map<StatusTriagem, Set<StatusTriagem>> TRANSICOES = Map.of(
            StatusTriagem.PENDENTE, EnumSet.of(StatusTriagem.EM_ATENDIMENTO, StatusTriagem.CANCELADA),
            StatusTriagem.EM_ATENDIMENTO, EnumSet.of(StatusTriagem.FINALIZADA, StatusTriagem.CANCELADA, StatusTriagem.PENDENTE),
            StatusTriagem.FINALIZADA, EnumSet.noneOf(StatusTriagem.class),
            StatusTriagem.CANCELADA, EnumSet.noneOf(StatusTriagem.class));

    private static final Set<StatusTriagem> EM_ANDAMENTO = EnumSet.of(StatusTriagem.PENDENTE, StatusTriagem.EM_ATENDIMENTO);

    /**
     * Ordem da fila: primeiro quem está em atendimento, depois quem espera; na espera, quem ainda não foi
     * avaliado (a enfermagem precisa ver), depois do risco mais grave para o mais leve e, no mesmo risco,
     * quem chegou primeiro. Triagens encerradas vêm por último, das mais recentes para as mais antigas.
     */
    static final Comparator<Triagem> ORDEM_DA_FILA = Comparator
            .comparingInt((Triagem t) -> switch (t.getStatus()) {
                case EM_ATENDIMENTO -> 0;
                case PENDENTE -> 1;
                default -> 2;
            })
            .thenComparingInt(t -> t.getStatus().encerrada() ? 0 : pesoRisco(t.getClassificacaoRisco()))
            .thenComparing((a, b) -> a.getStatus().encerrada()
                    ? b.getAtualizadaEm().compareTo(a.getAtualizadaEm())
                    : a.getAbertaEm().compareTo(b.getAbertaEm()));

    private final TriagemRepository repository;
    private final PacienteClient pacientes;
    private final RegistroDeEventos eventos;
    private final Clock relogio;

    @Autowired
    public TriagemService(TriagemRepository repository, PacienteClient pacientes, RegistroDeEventos eventos) {
        this(repository, pacientes, eventos, Clock.systemDefaultZone());
    }

    /** Usado nos testes, para fixar o "agora". */
    TriagemService(TriagemRepository repository, PacienteClient pacientes, RegistroDeEventos eventos, Clock relogio) {
        this.repository = repository;
        this.pacientes = pacientes;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    /** RF14: o paciente chegou e entra na fila, com status PENDENTE (RN36). */
    @Transactional
    public TriagemResponse abrir(AberturaRequest req, Profissional quem, String authorization) {
        PacienteResumo paciente = pacientes.buscar(req.pacienteId(), authorization);
        if (repository.existsByPacienteIdAndStatusIn(req.pacienteId(), EM_ANDAMENTO)) {
            throw new RegraNegocioException("Este paciente já tem uma triagem em andamento.");
        }
        Triagem triagem = repository.save(
                new Triagem(paciente.id(), paciente.nome(), req.queixaPrincipal().trim(), quem, agora()));
        Map<String, Object> dados = dadosDo(triagem);
        dados.put("queixaPrincipal", triagem.getQueixaPrincipal());
        eventos.registrar("TRIAGEM_ABERTA", triagem.getPacienteId(), triagem.getPacienteNome(), quem, dados);
        return TriagemResponse.de(triagem);
    }

    /** RF16: consulta por status e/ou paciente, na ordem da fila. */
    @Transactional(readOnly = true)
    public List<TriagemResponse> listar(StatusTriagem status, Integer pacienteId) {
        List<Triagem> triagens;
        if (pacienteId != null) {
            triagens = repository.findByPacienteIdOrderByAbertaEmDesc(pacienteId).stream()
                    .filter(t -> status == null || t.getStatus() == status)
                    .toList();
            return triagens.stream().map(TriagemResponse::de).toList();
        }
        triagens = status != null ? repository.findByStatus(status) : repository.findAll();
        return triagens.stream().sorted(ORDEM_DA_FILA).map(TriagemResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public TriagemResponse buscar(Integer id) {
        return TriagemResponse.de(buscarEntidade(id));
    }

    /** RF14/RF15: a enfermagem registra sintomas, sinais vitais e a classificação de risco. */
    @Transactional
    public TriagemResponse avaliar(Integer id, AvaliacaoRequest req, Profissional enfermeiro) {
        Triagem triagem = buscarEntidade(id);
        exigirVersaoAtual(triagem, req.versao());
        if (triagem.getStatus() != StatusTriagem.PENDENTE) {
            throw new RegraNegocioException("Só é possível avaliar uma triagem que está aguardando (pendente).");
        }
        triagem.avaliar(req.sintomas().trim(), textoOuNulo(req.pressaoArterial()), req.temperatura(),
                req.frequenciaCardiaca(), req.saturacao(), req.classificacaoRisco(), enfermeiro, agora());
        Triagem salva = repository.saveAndFlush(triagem);
        Map<String, Object> dados = dadosDo(salva);
        dados.put("queixaPrincipal", salva.getQueixaPrincipal());
        eventos.registrar("TRIAGEM_AVALIADA", salva.getPacienteId(), salva.getPacienteNome(), enfermeiro, dados);
        return TriagemResponse.de(salva);
    }

    /** RF16: muda o status seguindo as regras RN39, RN40 e RN41. */
    @Transactional
    public TriagemResponse mudarStatus(Integer id, StatusRequest req, Profissional quem) {
        Triagem triagem = buscarEntidade(id);
        exigirVersaoAtual(triagem, req.versao());

        StatusTriagem atual = triagem.getStatus();
        StatusTriagem novo = req.status();
        if (atual == novo) {
            throw new RegraNegocioException("A triagem já está com este status.");
        }
        if (atual.encerrada()) {
            throw new RegraNegocioException("Esta triagem já foi encerrada e não pode mudar de status.");
        }
        if (!TRANSICOES.get(atual).contains(novo)) {
            throw new RegraNegocioException("Para finalizar, a triagem precisa estar em atendimento.");
        }
        if (novo != StatusTriagem.CANCELADA && !"MEDICO".equals(quem.perfil())) {
            throw new AcaoNaoPermitidaException(
                    "Só o médico chama o paciente para atendimento, devolve à fila ou finaliza a triagem.");
        }
        if ((novo == StatusTriagem.EM_ATENDIMENTO || novo == StatusTriagem.FINALIZADA)
                && triagem.getClassificacaoRisco() == null) {
            throw new RegraNegocioException(
                    "A triagem ainda não tem classificação de risco: a enfermagem precisa avaliar o paciente primeiro.");
        }
        triagem.mudarStatus(novo, quem, agora());
        Triagem salva = repository.saveAndFlush(triagem);
        Map<String, Object> dados = dadosDo(salva);
        dados.put("statusAnterior", atual.name());
        eventos.registrar("TRIAGEM_STATUS_ALTERADO", salva.getPacienteId(), salva.getPacienteNome(), quem, dados);
        return TriagemResponse.de(salva);
    }

    /** Dados comuns a todos os eventos da triagem. */
    private static Map<String, Object> dadosDo(Triagem t) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("triagemId", t.getId());
        dados.put("status", t.getStatus().name());
        dados.put("classificacaoRisco", t.getClassificacaoRisco() == null ? null : t.getClassificacaoRisco().name());
        return dados;
    }

    private Triagem buscarEntidade(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Triagem " + id + " não encontrada"));
    }

    /** RN54: só aceita a mudança de quem estava vendo a versão mais recente. */
    private static void exigirVersaoAtual(Triagem triagem, Long versaoEnviada) {
        if (versaoEnviada == null || !versaoEnviada.equals(triagem.getVersao())) {
            throw new ConflitoEdicaoException();
        }
    }

    /** Sem classificação vem antes (precisa de avaliação); depois do mais grave para o mais leve. */
    private static int pesoRisco(ClassificacaoRisco risco) {
        if (risco == null) return 0;
        return switch (risco) {
            case CRITICO -> 1;
            case ALTO -> 2;
            case MEDIO -> 3;
            case BAIXO -> 4;
        };
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    private static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
