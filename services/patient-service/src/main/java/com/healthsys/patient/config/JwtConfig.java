package com.healthsys.patient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Validação local do JWT emitido pelo auth-service.
 * Como o segredo é compartilhado, este serviço não depende do auth-service estar no ar
 * para atender requisições: não há ponto único de falha na autenticação.
 */
@Configuration
public class JwtConfig {

    /** Deve ser igual ao emissor configurado no auth-service. */
    public static final String ISSUER = "healthsys-auth-service";

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret (JWT_SECRET) deve ter pelo menos 32 caracteres");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(bytes, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }
}
