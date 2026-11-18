package com.healthsys.record.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Regras de acesso do prontuário (dado clínico é sigiloso: RN25, RN29, RN31, RN33).
 * - Consultar o prontuário: MEDICO e ENFERMEIRO.
 * - Registrar consulta, solicitar exame, prescrever e suspender medicamento: MEDICO.
 * - Registrar o resultado de um exame: MEDICO e ENFERMEIRO.
 * A ordem importa: a primeira regra que combinar com a requisição é a que vale.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ErrosSegurancaHandler erros) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/prontuarios/*/exames/*/resultado").hasAnyRole("MEDICO", "ENFERMEIRO")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/prontuarios/*/consultas",
                                "/api/v1/prontuarios/*/exames",
                                "/api/v1/prontuarios/*/medicamentos").hasRole("MEDICO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/prontuarios/*/medicamentos/*/suspensao").hasRole("MEDICO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/prontuarios/**").hasAnyRole("MEDICO", "ENFERMEIRO")
                        // Qualquer outra rota do prontuário não prevista acima fica bloqueada.
                        .requestMatchers("/api/v1/prontuarios/**").denyAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorPerfil()))
                        .authenticationEntryPoint(erros)
                        .accessDeniedHandler(erros))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(erros)
                        .accessDeniedHandler(erros));
        return http.build();
    }

    /** Converte o claim "perfil" do token (ex.: "MEDICO") na autoridade ROLE_MEDICO. */
    static JwtAuthenticationConverter conversorPerfil() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("perfil");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}
