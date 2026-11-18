package com.healthsys.triage.config;

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
 * Regras de acesso da triagem (RN37, RN41), pensadas como num pronto-atendimento:
 * - Abrir a triagem quando o paciente chega: RECEPCIONISTA e ENFERMEIRO.
 * - Avaliar (sintomas, sinais vitais e classificação de risco): ENFERMEIRO.
 * - Mudar o status: RECEPCIONISTA, ENFERMEIRO e MEDICO; o TriagemService decide quem pode cada mudança.
 * - Consultar a fila: RECEPCIONISTA, ENFERMEIRO e MEDICO. O administrador não acessa dados clínicos.
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
                        .requestMatchers(HttpMethod.POST, "/api/v1/triagens").hasAnyRole("RECEPCIONISTA", "ENFERMEIRO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/triagens/*/avaliacao").hasRole("ENFERMEIRO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/triagens/*/status").hasAnyRole("RECEPCIONISTA", "ENFERMEIRO", "MEDICO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/triagens", "/api/v1/triagens/*").hasAnyRole("RECEPCIONISTA", "ENFERMEIRO", "MEDICO")
                        // Qualquer outra rota de triagem não prevista acima fica bloqueada.
                        .requestMatchers("/api/v1/triagens", "/api/v1/triagens/**").denyAll()
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
