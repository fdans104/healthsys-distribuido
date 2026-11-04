package com.healthsys.auth.auth;

import com.healthsys.auth.auth.dto.LoginRequest;
import com.healthsys.auth.auth.dto.LoginResponse;
import com.healthsys.auth.config.JwtConfig;
import com.healthsys.auth.exception.CredenciaisInvalidasException;
import com.healthsys.auth.usuario.Perfil;
import com.healthsys.auth.usuario.Usuario;
import com.healthsys.auth.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UsuarioRepository repository;
    private JwtDecoder decoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        JwtConfig config = new JwtConfig();
        SecretKey chave = JwtConfig.chave("segredo-de-teste-com-mais-de-32-caracteres");
        decoder = config.jwtDecoder(chave);
        service = new AuthService(repository, encoder, config.jwtEncoder(chave), 8);

        Usuario ana = new Usuario("Dra. Ana", Perfil.MEDICO, "ana@hospital.com", encoder.encode("senhaForte123"));
        ReflectionTestUtils.setField(ana, "id", 7);
        when(repository.findByEmail("ana@hospital.com")).thenReturn(Optional.of(ana));
        when(repository.findByEmail("ninguem@hospital.com")).thenReturn(Optional.empty());
    }

    @Test
    void loginValidoEmiteTokenComPerfilEId() {
        LoginResponse resp = service.login(new LoginRequest(" Ana@Hospital.COM ", "senhaForte123"));

        Jwt jwt = decoder.decode(resp.token()); // também valida assinatura, expiração e emissor
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("perfil")).isEqualTo("MEDICO");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("ana@hospital.com");
        assertThat(resp.tipo()).isEqualTo("Bearer");
        assertThat(resp.expiraEm()).isAfter(Instant.now());
        assertThat(resp.usuario().id()).isEqualTo(7);
    }

    @Test
    void senhaErradaEhRejeitada() {
        assertThatThrownBy(() -> service.login(new LoginRequest("ana@hospital.com", "errada123")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void emailInexistenteEhRejeitadoComMesmaMensagem() {
        assertThatThrownBy(() -> service.login(new LoginRequest("ninguem@hospital.com", "qualquer123")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha incorretos");
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRecusado() {
        LoginResponse resp = service.login(new LoginRequest("ana@hospital.com", "senhaForte123"));
        JwtDecoder outroDecoder = new JwtConfig().jwtDecoder(JwtConfig.chave("um-segredo-totalmente-diferente-do-original"));

        assertThatThrownBy(() -> outroDecoder.decode(resp.token())).isInstanceOf(Exception.class);
    }
}
