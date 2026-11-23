package com.healthsys.monitoring.config;

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
 * Regras de acesso do monitoramento, pensadas como num hospital:
 * - Ver os leitos e a ocupação: todos os perfis (o administrador vê só a ocupação, sem o nome do paciente).
 * - Cadastrar e remover leitos (estrutura do hospital): ADMIN.
 * - Internar e dar alta do leito: MEDICO e ENFERMEIRO. Marcar como higienizado: ENFERMEIRO.
 * - Sinais vitais ao vivo e alertas: MEDICO e ENFERMEIRO (dado clínico).
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
                        .requestMatchers(HttpMethod.GET, "/api/v1/leitos", "/api/v1/leitos/ocupacao")
                                .hasAnyRole("ADMIN", "RECEPCIONISTA", "MEDICO", "ENFERMEIRO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/leitos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/leitos/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/leitos/*/ocupar", "/api/v1/leitos/*/liberar")
                                .hasAnyRole("MEDICO", "ENFERMEIRO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/leitos/*/higienizado").hasRole("ENFERMEIRO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/monitoramento/**").hasAnyRole("MEDICO", "ENFERMEIRO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/monitoramento/alertas/*/reconhecer").hasAnyRole("MEDICO", "ENFERMEIRO")
                        // Qualquer outra rota destes recursos não prevista acima fica bloqueada.
                        .requestMatchers("/api/v1/leitos", "/api/v1/leitos/**", "/api/v1/monitoramento/**").denyAll()
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
