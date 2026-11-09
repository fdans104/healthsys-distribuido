package com.healthsys.patient.paciente;

import com.healthsys.patient.exception.ConflitoEdicaoException;
import com.healthsys.patient.exception.CpfJaCadastradoException;
import com.healthsys.patient.exception.RecursoNaoEncontradoException;
import com.healthsys.patient.paciente.dto.PacienteRequest;
import com.healthsys.patient.paciente.dto.PacienteResponse;
import com.healthsys.patient.validacao.CpfUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Regras de negócio do cadastro de pacientes. */
@Service
public class PacienteService {

    /** Busca composta só por dígitos e pontuação de CPF é tratada como busca por CPF. */
    private static final Pattern PARECE_CPF = Pattern.compile("[\\d.\\-\\s]+");

    private final PacienteRepository repository;

    public PacienteService(PacienteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PacienteResponse criar(PacienteRequest req) {
        String cpf = CpfUtils.somenteDigitos(req.cpf());
        if (repository.existsByCpf(cpf)) {
            throw new CpfJaCadastradoException();
        }
        Paciente paciente = new Paciente(
                req.nome().trim(),
                cpf,
                req.dataNascimento(),
                req.sexo(),
                textoOuNulo(req.telefone()),
                emailOuNulo(req.email()),
                textoOuNulo(req.endereco())
        );
        return PacienteResponse.de(repository.save(paciente));
    }

    /** Sem busca: todos em ordem alfabética. Com busca: por trecho do nome ou início do CPF. */
    @Transactional(readOnly = true)
    public List<PacienteResponse> listar(String busca) {
        List<Paciente> pacientes;
        if (busca == null || busca.isBlank()) {
            pacientes = repository.findAllByOrderByNomeAsc();
        } else {
            String termo = busca.trim();
            String digitos = CpfUtils.somenteDigitos(termo);
            pacientes = PARECE_CPF.matcher(termo).matches() && !digitos.isEmpty()
                    ? repository.findByCpfStartingWithOrderByNomeAsc(digitos)
                    : repository.findByNomeContainingIgnoreCaseOrderByNomeAsc(termo);
        }
        return pacientes.stream().map(PacienteResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PacienteResponse buscarPorId(Integer id) {
        return PacienteResponse.de(buscarEntidade(id));
    }

    @Transactional
    public PacienteResponse atualizar(Integer id, PacienteRequest req) {
        Paciente paciente = buscarEntidade(id);

        // RN54: só aceita a alteração de quem estava vendo a versão mais recente do registro.
        // Se outra pessoa salvou antes, o número de edição já subiu e não bate com o que a tela enviou.
        if (req.versao() == null || !req.versao().equals(paciente.getVersao())) {
            throw new ConflitoEdicaoException();
        }

        String cpf = CpfUtils.somenteDigitos(req.cpf());
        if (repository.existsByCpfAndIdNot(cpf, id)) {
            throw new CpfJaCadastradoException();
        }

        paciente.setNome(req.nome().trim());
        paciente.setCpf(cpf);
        paciente.setDataNascimento(req.dataNascimento());
        paciente.setSexo(req.sexo());
        paciente.setTelefone(textoOuNulo(req.telefone()));
        paciente.setEmail(emailOuNulo(req.email()));
        paciente.setEndereco(textoOuNulo(req.endereco()));
        // saveAndFlush dispara o @PreUpdate agora, para a resposta já sair com "atualizadoEm".
        return PacienteResponse.de(repository.saveAndFlush(paciente));
    }

    @Transactional
    public void remover(Integer id) {
        repository.delete(buscarEntidade(id));
    }

    private Paciente buscarEntidade(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente " + id + " não encontrado"));
    }

    private static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String emailOuNulo(String valor) {
        String email = textoOuNulo(valor);
        return email == null ? null : email.toLowerCase(Locale.ROOT);
    }
}