package com.healthsys.patient.clinico;

import com.healthsys.patient.clinico.dto.AlergiaRequest;
import com.healthsys.patient.clinico.dto.AlergiaResponse;
import com.healthsys.patient.clinico.dto.AtendimentoRequest;
import com.healthsys.patient.clinico.dto.AtendimentoResponse;
import com.healthsys.patient.clinico.dto.HistoricoItem;
import com.healthsys.patient.clinico.dto.VacinaRequest;
import com.healthsys.patient.clinico.dto.VacinaResponse;
import com.healthsys.patient.exception.RecursoNaoEncontradoException;
import com.healthsys.patient.paciente.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Regras da ficha clínica do paciente: alergias (RF08), vacinas (RF09), atendimentos (RF06)
 * e o histórico clínico em ordem de data (RF07).
 * Todo registro fica vinculado a um paciente existente e guarda quem registrou.
 * Registros de prontuário não são editados nem apagados: uma correção é um novo registro.
 * Quem pode fazer o quê está em SecurityConfig.
 */
@Service
public class ClinicoService {

    private final PacienteRepository pacientes;
    private final AlergiaRepository alergias;
    private final VacinaRepository vacinas;
    private final AtendimentoRepository atendimentos;

    public ClinicoService(PacienteRepository pacientes, AlergiaRepository alergias,
                          VacinaRepository vacinas, AtendimentoRepository atendimentos) {
        this.pacientes = pacientes;
        this.alergias = alergias;
        this.vacinas = vacinas;
        this.atendimentos = atendimentos;
    }

    // ---------- Alergias (RF08) ----------

    @Transactional
    public AlergiaResponse registrarAlergia(Integer pacienteId, AlergiaRequest req, Profissional profissional) {
        exigirPaciente(pacienteId);
        Alergia alergia = new Alergia(pacienteId, req.descricao().trim(), profissional);
        return AlergiaResponse.de(alergias.save(alergia));
    }

    @Transactional(readOnly = true)
    public List<AlergiaResponse> listarAlergias(Integer pacienteId) {
        exigirPaciente(pacienteId);
        return alergias.findByPacienteIdOrderByRegistradoEmDescIdDesc(pacienteId).stream()
                .map(AlergiaResponse::de).toList();
    }

    // ---------- Vacinas (RF09) ----------

    @Transactional
    public VacinaResponse registrarVacina(Integer pacienteId, VacinaRequest req, Profissional profissional) {
        exigirPaciente(pacienteId);
        Vacina vacina = new Vacina(pacienteId, req.nomeVacina().trim(), req.dataAplicacao(),
                textoOuNulo(req.dose()), textoOuNulo(req.lote()), profissional);
        return VacinaResponse.de(vacinas.save(vacina));
    }

    @Transactional(readOnly = true)
    public List<VacinaResponse> listarVacinas(Integer pacienteId) {
        exigirPaciente(pacienteId);
        return vacinas.findByPacienteIdOrderByDataAplicacaoDescIdDesc(pacienteId).stream()
                .map(VacinaResponse::de).toList();
    }

    // ---------- Atendimentos (RF06) ----------

    @Transactional
    public AtendimentoResponse registrarAtendimento(Integer pacienteId, AtendimentoRequest req, Profissional profissional) {
        exigirPaciente(pacienteId);
        Atendimento atendimento = new Atendimento(pacienteId, req.tipo(), req.data(),
                textoOuNulo(req.observacoes()), profissional);
        return AtendimentoResponse.de(atendimentos.save(atendimento));
    }

    @Transactional(readOnly = true)
    public List<AtendimentoResponse> listarAtendimentos(Integer pacienteId) {
        exigirPaciente(pacienteId);
        return atendimentos.findByPacienteIdOrderByDataDescIdDesc(pacienteId).stream()
                .map(AtendimentoResponse::de).toList();
    }

    // ---------- Histórico clínico (RF07) ----------

    /** Atendimentos, vacinas e alergias juntos, do mais recente para o mais antigo. Lista vazia = sem registros. */
    @Transactional(readOnly = true)
    public List<HistoricoItem> historico(Integer pacienteId) {
        exigirPaciente(pacienteId);
        List<HistoricoItem> itens = new ArrayList<>();

        for (Atendimento a : atendimentos.findByPacienteIdOrderByDataDescIdDesc(pacienteId)) {
            itens.add(new HistoricoItem("ATENDIMENTO", a.getId(), a.getData(),
                    "Atendimento: " + rotulo(a.getTipo()), a.getObservacoes(),
                    a.getRegistradoPorNome(), a.getRegistradoPorPerfil()));
        }
        for (Vacina v : vacinas.findByPacienteIdOrderByDataAplicacaoDescIdDesc(pacienteId)) {
            itens.add(new HistoricoItem("VACINA", v.getId(), v.getDataAplicacao().atStartOfDay(),
                    "Vacina: " + v.getNomeVacina(), detalheVacina(v),
                    v.getRegistradoPorNome(), v.getRegistradoPorPerfil()));
        }
        for (Alergia al : alergias.findByPacienteIdOrderByRegistradoEmDescIdDesc(pacienteId)) {
            itens.add(new HistoricoItem("ALERGIA", al.getId(), al.getRegistradoEm(),
                    "Alergia registrada: " + al.getDescricao(), null,
                    al.getRegistradoPorNome(), al.getRegistradoPorPerfil()));
        }

        itens.sort(Comparator.comparing(HistoricoItem::data,
                Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())));
        return itens;
    }

    /** Usado na exclusão: paciente com prontuário não pode ser apagado. */
    @Transactional(readOnly = true)
    public boolean possuiRegistrosClinicos(Integer pacienteId) {
        return alergias.existsByPacienteId(pacienteId)
                || vacinas.existsByPacienteId(pacienteId)
                || atendimentos.existsByPacienteId(pacienteId);
    }

    private void exigirPaciente(Integer pacienteId) {
        if (!pacientes.existsById(pacienteId)) {
            throw new RecursoNaoEncontradoException("Paciente " + pacienteId + " não encontrado");
        }
    }

    private static String detalheVacina(Vacina v) {
        List<String> partes = new ArrayList<>();
        if (v.getDose() != null) partes.add(v.getDose());
        if (v.getLote() != null) partes.add("lote " + v.getLote());
        return partes.isEmpty() ? null : String.join(" · ", partes);
    }

    private static String rotulo(TipoAtendimento tipo) {
        return switch (tipo) {
            case CONSULTA -> "consulta";
            case RETORNO -> "retorno";
            case URGENCIA -> "urgência";
            case EMERGENCIA -> "emergência";
        };
    }

    private static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
