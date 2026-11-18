package com.healthsys.triage.triagem;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Quem está fazendo o registro clínico, lido do token JWT do usuário logado.
 * Fica gravado em cada registro, como a assinatura de um profissional no prontuário.
 */
public record Profissional(Integer id, String nome, String perfil) {

    public static Profissional doToken(Jwt jwt) {
        Integer id = null;
        try {
            id = Integer.valueOf(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            // token sem id numérico: o registro fica só com nome e perfil
        }
        return new Profissional(id, jwt.getClaimAsString("nome"), jwt.getClaimAsString("perfil"));
    }
}
