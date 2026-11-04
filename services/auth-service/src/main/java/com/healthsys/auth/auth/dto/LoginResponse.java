package com.healthsys.auth.auth.dto;

import com.healthsys.auth.usuario.dto.UsuarioResponse;

import java.time.Instant;

/** Resposta do login: o token deve ser enviado como "Authorization: Bearer <token>". */
public record LoginResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioResponse usuario
) {
}
