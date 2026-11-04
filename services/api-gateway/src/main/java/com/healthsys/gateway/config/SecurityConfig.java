package com.healthsys.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Primeira barreira de segurança: o gateway só deixa passar requisições com JWT válido
 * (exceto o login). As regras por perfil (ADMIN, MEDICO...) continuam em cada microsserviço,
 * que também revalida o token (defesa em profundidade).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /** Deve ser igual ao emissor configurado no auth-service. */
    static final String ISSUER = "healthsys-auth-service";

    private static final byte[] CORPO_401 = ("{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
            + "\"detail\":\"Token ausente, inválido ou expirado. Faça login novamente.\"}")
            .getBytes(StandardCharsets.UTF_8);

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
        ServerAuthenticationEntryPoint naoAutenticado = naoAutenticado();
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(naoAutenticado)
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder)))
                .exceptionHandling(e -> e.authenticationEntryPoint(naoAutenticado));
        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.secret}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret (JWT_SECRET) deve ter pelo menos 32 caracteres");
        }
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withSecretKey(new SecretKeySpec(bytes, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    /** 401 no mesmo formato Problem Details (RFC 7807) dos microsserviços. */
    private static ServerAuthenticationEntryPoint naoAutenticado() {
        return (exchange, ex) -> {
            ServerHttpResponse res = exchange.getResponse();
            res.setStatusCode(HttpStatus.UNAUTHORIZED);
            res.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
            res.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            DataBuffer corpo = res.bufferFactory().wrap(CORPO_401);
            return res.writeWith(Mono.just(corpo));
        };
    }
}
