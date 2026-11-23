package com.healthsys.notification.notificacao;

import org.springframework.security.oauth2.jwt.Jwt;

/** Quem está logado, lido do token JWT: define quais notificações a pessoa recebe. */
public record Profissional(Integer id, String nome, String perfil) {

    public static Profissional doToken(Jwt jwt) {
        Integer id = null;
        try {
            id = Integer.valueOf(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            // token sem id numérico: recebe só as notificações do perfil
        }
        return new Profissional(id, jwt.getClaimAsString("nome"), jwt.getClaimAsString("perfil"));
    }
}
