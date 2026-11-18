package com.healthsys.patient.config;

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
 * Regras de acesso do patient-service, pensadas como num hospital:
 * - Buscar pacientes e ver os dados cadastrais: todos os perfis.
 * - Cadastrar e editar os dados cadastrais: RECEPCIONISTA (é a recepção que abre e atualiza a ficha).
 * - Excluir paciente: ADMIN, e só se ele não tiver registros clínicos (ver PacienteService).
 * - Histórico clínico, alergias, vacinas e atendimentos: só MEDICO e ENFERMEIRO (dado clínico é sigiloso).
 *   - Registrar atendimento: MEDICO.
 *   - Registrar vacina aplicada: ENFERMEIRO.
 *   - Registrar alergia: MEDICO e ENFERMEIRO.
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
                        // Ficha clínica
                        .requestMatchers(HttpMethod.POST, "/api/v1/pacientes/*/atendimentos").hasRole("MEDICO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pacientes/*/vacinas").hasRole("ENFERMEIRO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pacientes/*/alergias").hasAnyRole("MEDICO", "ENFERMEIRO")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/pacientes/*/historico",
                                "/api/v1/pacientes/*/alergias",
                                "/api/v1/pacientes/*/vacinas",
                                "/api/v1/pacientes/*/atendimentos").hasAnyRole("MEDICO", "ENFERMEIRO")
                        // Cadastro do paciente
                        .requestMatchers(HttpMethod.POST, "/api/v1/pacientes").hasRole("RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/pacientes/*").hasRole("RECEPCIONISTA")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/pacientes/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pacientes", "/api/v1/pacientes/*")
                                .hasAnyRole("ADMIN", "RECEPCIONISTA", "MEDICO", "ENFERMEIRO")
                        // Qualquer outra rota de pacientes não prevista acima fica bloqueada.
                        .requestMatchers("/api/v1/pacientes", "/api/v1/pacientes/**").denyAll()
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
