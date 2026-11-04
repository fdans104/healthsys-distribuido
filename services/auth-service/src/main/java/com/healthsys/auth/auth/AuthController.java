package com.healthsys.auth.auth;

import com.healthsys.auth.auth.dto.LoginRequest;
import com.healthsys.auth.auth.dto.LoginResponse;
import com.healthsys.auth.usuario.UsuarioService;
import com.healthsys.auth.usuario.dto.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints REST de autenticação. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    /** Público: troca e-mail e senha por um token JWT. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** Autenticado: devolve os dados do dono do token (o "sub" do JWT é o id do usuário). */
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return usuarioService.buscarPorId(Integer.valueOf(jwt.getSubject()));
    }
}
