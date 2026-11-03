package com.healthsys.auth.usuario;

import com.healthsys.auth.exception.EmailJaCadastradoException;
import com.healthsys.auth.exception.RecursoNaoEncontradoException;
import com.healthsys.auth.usuario.dto.UsuarioCreateRequest;
import com.healthsys.auth.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private UsuarioRepository repository;
    private PasswordEncoder encoder;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        encoder = new BCryptPasswordEncoder();
        service = new UsuarioService(repository, encoder);
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void criarGravaHashDaSenhaENormalizaEmail() {
        var req = new UsuarioCreateRequest("  Dra. Ana  ", " Ana@Hospital.COM ", "senhaForte123", Perfil.MEDICO);

        UsuarioResponse resp = service.criar(req);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        Usuario salvo = captor.getValue();

        assertThat(salvo.getEmail()).isEqualTo("ana@hospital.com");
        assertThat(salvo.getNome()).isEqualTo("Dra. Ana");
        assertThat(salvo.getSenhaHash()).isNotEqualTo("senhaForte123");
        assertThat(encoder.matches("senhaForte123", salvo.getSenhaHash())).isTrue();
        assertThat(resp.perfil()).isEqualTo(Perfil.MEDICO);
    }

    @Test
    void criarRejeitaEmailDuplicado() {
        when(repository.existsByEmail("ana@hospital.com")).thenReturn(true);
        var req = new UsuarioCreateRequest("Ana", "ana@hospital.com", "senhaForte123", Perfil.MEDICO);

        assertThatThrownBy(() -> service.criar(req)).isInstanceOf(EmailJaCadastradoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void buscarInexistenteLancaNaoEncontrado() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
