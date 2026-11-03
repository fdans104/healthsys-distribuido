package com.healthsys.auth.usuario.dto;

import com.healthsys.auth.usuario.Perfil;
import com.healthsys.auth.usuario.Usuario;

import java.time.LocalDateTime;

/** O que a API devolve. Repare que a senha (nem o hash) nunca sai daqui. */
public record UsuarioResponse(
        Integer id,
        String nome,
        String email,
        Perfil perfil,
        LocalDateTime criadoEm
) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.getCriadoEm());
    }
}
