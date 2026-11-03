package com.healthsys.auth.usuario;

import com.healthsys.auth.exception.EmailJaCadastradoException;
import com.healthsys.auth.exception.RecursoNaoEncontradoException;
import com.healthsys.auth.usuario.dto.UsuarioCreateRequest;
import com.healthsys.auth.usuario.dto.UsuarioResponse;
import com.healthsys.auth.usuario.dto.UsuarioUpdateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Regras de negócio do cadastro de usuários. */
@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse criar(UsuarioCreateRequest req) {
        String email = normalizarEmail(req.email());
        if (repository.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }
        Usuario usuario = new Usuario(
                req.nome().trim(),
                req.perfil(),
                email,
                passwordEncoder.encode(req.senha())
        );
        return UsuarioResponse.de(repository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repository.findAll().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Integer id) {
        return UsuarioResponse.de(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse atualizar(Integer id, UsuarioUpdateRequest req) {
        Usuario usuario = buscarEntidade(id);
        String email = normalizarEmail(req.email());

        // Só é conflito se o e-mail pertencer a OUTRO usuário.
        if (!email.equals(usuario.getEmail()) && repository.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }

        usuario.setNome(req.nome().trim());
        usuario.setEmail(email);
        usuario.setPerfil(req.perfil());
        if (req.senha() != null) {
            usuario.setSenhaHash(passwordEncoder.encode(req.senha()));
        }
        // Sem chamar save(): a entidade está "gerenciada" pelo JPA dentro da
        // transação, e as alterações são gravadas automaticamente no commit.
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public void remover(Integer id) {
        repository.delete(buscarEntidade(id));
    }

    private Usuario buscarEntidade(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado"));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
