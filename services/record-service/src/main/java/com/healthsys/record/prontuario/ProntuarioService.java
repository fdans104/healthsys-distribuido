package com.healthsys.record.prontuario;

import com.healthsys.record.eventos.RegistroDeEventos;
import com.healthsys.record.exception.RecursoNaoEncontradoException;
import com.healthsys.record.exception.RegraNegocioException;
import com.healthsys.record.paciente.PacienteClient;
import com.healthsys.record.paciente.PacienteResumo;
import com.healthsys.record.prontuario.dto.ConsultaRequest;
import com.healthsys.record.prontuario.dto.ExameRequest;
import com.healthsys.record.prontuario.dto.MedicamentoRequest;
import com.healthsys.record.prontuario.dto.ProntuarioResponse;
import com.healthsys.record.prontuario.dto.ResultadoExameRequest;
import com.healthsys.record.prontuario.dto.SuspensaoRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras do prontuário eletrônico (RF10 a RF13).
 * - Todo registro fica ligado a um paciente que existe no patient-service (RN24, RN27, RN30, RN32).
 * - Todo registro guarda quem registrou e quando.
 * - Nada é apagado: exame recebe resultado uma vez; medicamento é suspenso com motivo.
 * Cada registro gera um evento no tópico "prontuario.eventos" (Kafka). O notification-service avisa,
 * por exemplo, a enfermagem de uma nova prescrição e o médico de que o resultado do exame saiu.
 */
@Service
public class ProntuarioService {

    private final ConsultaRepository consultas;
    private final ExameRepository exames;
    private final MedicamentoRepository medicamentos;
    private final PacienteClient pacientes;
    private final RegistroDeEventos eventos;
    private final Clock relogio;

    @Autowired
    public ProntuarioService(ConsultaRepository consultas, ExameRepository exames, MedicamentoRepository medicamentos,
                             PacienteClient pacientes, RegistroDeEventos eventos) {
        this(consultas, exames, medicamentos, pacientes, eventos, Clock.systemDefaultZone());
    }

    /** Usado nos testes, para fixar o "agora". */
    ProntuarioService(ConsultaRepository consultas, ExameRepository exames, MedicamentoRepository medicamentos,
                      PacienteClient pacientes, RegistroDeEventos eventos, Clock relogio) {
        this.consultas = consultas;
        this.exames = exames;
        this.medicamentos = medicamentos;
        this.pacientes = pacientes;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    /** RF10: o prontuário completo do paciente. Lista vazia = nenhum registro daquele tipo. */
    public ProntuarioResponse prontuario(Integer pacienteId) {
        return new ProntuarioResponse(pacienteId, listarConsultas(pacienteId), listarExames(pacienteId),
                listarMedicamentos(pacienteId));
    }

    // ---------- Consultas (RF11) ----------

    public Consulta registrarConsulta(Integer pacienteId, ConsultaRequest req, Profissional medico, String authorization) {
        PacienteResumo paciente = pacientes.buscar(pacienteId, authorization);
        Consulta consulta = consultas.save(new Consulta(pacienteId, req.data(), req.motivo().trim(),
                textoOuNulo(req.avaliacao()), textoOuNulo(req.diagnostico()), textoOuNulo(req.conduta()), medico, agora()));
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("consultaId", consulta.getId());
        dados.put("motivo", consulta.getMotivo());
        eventos.registrar("CONSULTA_REGISTRADA", pacienteId, paciente.nome(), medico, dados);
        return consulta;
    }

    public List<Consulta> listarConsultas(Integer pacienteId) {
        return consultas.findByPacienteIdOrderByDataDesc(pacienteId);
    }

    // ---------- Exames (RF12) ----------

    public Exame solicitarExame(Integer pacienteId, ExameRequest req, Profissional medico, String authorization) {
        PacienteResumo paciente = pacientes.buscar(pacienteId, authorization);
        Exame exame = new Exame(pacienteId, req.nome().trim(), req.tipo(), textoOuNulo(req.observacoes()), medico, agora());
        exame.registrarNomeDoPaciente(paciente.nome());
        exame = exames.save(exame);
        eventos.registrar("EXAME_SOLICITADO", pacienteId, paciente.nome(), medico, dadosDoExame(exame));
        return exame;
    }

    public Exame registrarResultado(Integer pacienteId, String exameId, ResultadoExameRequest req, Profissional profissional) {
        Exame exame = exames.findByIdAndPacienteId(exameId, pacienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exame não encontrado no prontuário deste paciente"));
        if (exame.getStatus() == StatusExame.CONCLUIDO) {
            throw new RegraNegocioException("Este exame já tem resultado registrado e não pode ser alterado.");
        }
        exame.registrarResultado(req.resultado().trim(), req.dataResultado(), profissional, agora());
        exame = exames.save(exame);
        // Vai para quem pediu o exame: o notification-service avisa esse médico.
        eventos.registrar("EXAME_RESULTADO_REGISTRADO", pacienteId, exame.getPacienteNome(), profissional, dadosDoExame(exame));
        return exame;
    }

    public List<Exame> listarExames(Integer pacienteId) {
        return exames.findByPacienteIdOrderBySolicitadoEmDesc(pacienteId);
    }

    // ---------- Medicamentos (RF13) ----------

    public Medicamento prescrever(Integer pacienteId, MedicamentoRequest req, Profissional medico, String authorization) {
        if (req.dataFim() != null && req.dataFim().isBefore(req.dataInicio())) {
            throw new RegraNegocioException("A data de fim não pode ser anterior à data de início.");
        }
        PacienteResumo paciente = pacientes.buscar(pacienteId, authorization);
        Medicamento medicamento = new Medicamento(pacienteId, req.nome().trim(), req.dosagem().trim(), req.via(),
                req.frequencia().trim(), req.dataInicio(), req.dataFim(), textoOuNulo(req.observacoes()), medico, agora());
        medicamento.registrarNomeDoPaciente(paciente.nome());
        medicamento = medicamentos.save(medicamento);
        eventos.registrar("MEDICAMENTO_PRESCRITO", pacienteId, paciente.nome(), medico, dadosDoMedicamento(medicamento));
        return medicamento;
    }

    public Medicamento suspender(Integer pacienteId, String medicamentoId, SuspensaoRequest req, Profissional medico) {
        Medicamento medicamento = medicamentos.findByIdAndPacienteId(medicamentoId, pacienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Medicamento não encontrado no prontuário deste paciente"));
        if (medicamento.getStatus() == StatusMedicamento.SUSPENSO) {
            throw new RegraNegocioException("Este medicamento já está suspenso.");
        }
        medicamento.suspender(req.motivo().trim(), medico, agora());
        medicamento = medicamentos.save(medicamento);
        Map<String, Object> dados = dadosDoMedicamento(medicamento);
        dados.put("motivoSuspensao", medicamento.getMotivoSuspensao());
        eventos.registrar("MEDICAMENTO_SUSPENSO", pacienteId, medicamento.getPacienteNome(), medico, dados);
        return medicamento;
    }

    public List<Medicamento> listarMedicamentos(Integer pacienteId) {
        return medicamentos.findByPacienteIdOrderByPrescritoEmDesc(pacienteId);
    }

    private static Map<String, Object> dadosDoExame(Exame exame) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("exameId", exame.getId());
        dados.put("nome", exame.getNome());
        dados.put("tipo", exame.getTipo() == null ? null : exame.getTipo().name());
        dados.put("status", exame.getStatus() == null ? null : exame.getStatus().name());
        dados.put("solicitadoPorId", exame.getSolicitadoPorId());
        dados.put("solicitadoPorNome", exame.getSolicitadoPorNome());
        return dados;
    }

    private static Map<String, Object> dadosDoMedicamento(Medicamento m) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("medicamentoId", m.getId());
        dados.put("nome", m.getNome());
        dados.put("dosagem", m.getDosagem());
        dados.put("via", m.getVia() == null ? null : m.getVia().name());
        dados.put("frequencia", m.getFrequencia());
        return dados;
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    private static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
