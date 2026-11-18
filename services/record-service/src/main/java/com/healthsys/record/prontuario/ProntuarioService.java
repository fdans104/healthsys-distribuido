package com.healthsys.record.prontuario;

import com.healthsys.record.exception.RecursoNaoEncontradoException;
import com.healthsys.record.exception.RegraNegocioException;
import com.healthsys.record.paciente.PacienteClient;
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
import java.util.List;

/**
 * Regras do prontuário eletrônico (RF10 a RF13).
 * - Todo registro fica ligado a um paciente que existe no patient-service (RN24, RN27, RN30, RN32).
 * - Todo registro guarda quem registrou e quando.
 * - Nada é apagado: exame recebe resultado uma vez; medicamento é suspenso com motivo.
 */
@Service
public class ProntuarioService {

    private final ConsultaRepository consultas;
    private final ExameRepository exames;
    private final MedicamentoRepository medicamentos;
    private final PacienteClient pacientes;
    private final Clock relogio;

    @Autowired
    public ProntuarioService(ConsultaRepository consultas, ExameRepository exames, MedicamentoRepository medicamentos,
                             PacienteClient pacientes) {
        this(consultas, exames, medicamentos, pacientes, Clock.systemDefaultZone());
    }

    /** Usado nos testes, para fixar o "agora". */
    ProntuarioService(ConsultaRepository consultas, ExameRepository exames, MedicamentoRepository medicamentos,
                      PacienteClient pacientes, Clock relogio) {
        this.consultas = consultas;
        this.exames = exames;
        this.medicamentos = medicamentos;
        this.pacientes = pacientes;
        this.relogio = relogio;
    }

    /** RF10: o prontuário completo do paciente. Lista vazia = nenhum registro daquele tipo. */
    public ProntuarioResponse prontuario(Integer pacienteId) {
        return new ProntuarioResponse(pacienteId, listarConsultas(pacienteId), listarExames(pacienteId),
                listarMedicamentos(pacienteId));
    }

    // ---------- Consultas (RF11) ----------

    public Consulta registrarConsulta(Integer pacienteId, ConsultaRequest req, Profissional medico, String authorization) {
        pacientes.buscar(pacienteId, authorization);
        Consulta consulta = new Consulta(pacienteId, req.data(), req.motivo().trim(), textoOuNulo(req.avaliacao()),
                textoOuNulo(req.diagnostico()), textoOuNulo(req.conduta()), medico, agora());
        return consultas.save(consulta);
    }

    public List<Consulta> listarConsultas(Integer pacienteId) {
        return consultas.findByPacienteIdOrderByDataDesc(pacienteId);
    }

    // ---------- Exames (RF12) ----------

    public Exame solicitarExame(Integer pacienteId, ExameRequest req, Profissional medico, String authorization) {
        pacientes.buscar(pacienteId, authorization);
        Exame exame = new Exame(pacienteId, req.nome().trim(), req.tipo(), textoOuNulo(req.observacoes()), medico, agora());
        return exames.save(exame);
    }

    public Exame registrarResultado(Integer pacienteId, String exameId, ResultadoExameRequest req, Profissional profissional) {
        Exame exame = exames.findByIdAndPacienteId(exameId, pacienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exame não encontrado no prontuário deste paciente"));
        if (exame.getStatus() == StatusExame.CONCLUIDO) {
            throw new RegraNegocioException("Este exame já tem resultado registrado e não pode ser alterado.");
        }
        exame.registrarResultado(req.resultado().trim(), req.dataResultado(), profissional, agora());
        return exames.save(exame);
    }

    public List<Exame> listarExames(Integer pacienteId) {
        return exames.findByPacienteIdOrderBySolicitadoEmDesc(pacienteId);
    }

    // ---------- Medicamentos (RF13) ----------

    public Medicamento prescrever(Integer pacienteId, MedicamentoRequest req, Profissional medico, String authorization) {
        if (req.dataFim() != null && req.dataFim().isBefore(req.dataInicio())) {
            throw new RegraNegocioException("A data de fim não pode ser anterior à data de início.");
        }
        pacientes.buscar(pacienteId, authorization);
        Medicamento medicamento = new Medicamento(pacienteId, req.nome().trim(), req.dosagem().trim(), req.via(),
                req.frequencia().trim(), req.dataInicio(), req.dataFim(), textoOuNulo(req.observacoes()), medico, agora());
        return medicamentos.save(medicamento);
    }

    public Medicamento suspender(Integer pacienteId, String medicamentoId, SuspensaoRequest req, Profissional medico) {
        Medicamento medicamento = medicamentos.findByIdAndPacienteId(medicamentoId, pacienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Medicamento não encontrado no prontuário deste paciente"));
        if (medicamento.getStatus() == StatusMedicamento.SUSPENSO) {
            throw new RegraNegocioException("Este medicamento já está suspenso.");
        }
        medicamento.suspender(req.motivo().trim(), medico, agora());
        return medicamentos.save(medicamento);
    }

    public List<Medicamento> listarMedicamentos(Integer pacienteId) {
        return medicamentos.findByPacienteIdOrderByPrescritoEmDesc(pacienteId);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    private static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
