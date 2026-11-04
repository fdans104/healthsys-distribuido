package com.healthsys.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Documentação Swagger: http://localhost:8081/swagger-ui.html (botão "Authorize" aceita o token JWT). */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "HealthSys - Auth Service",
                version = "0.2.0",
                description = "Cadastro de usuários, login e emissão de tokens JWT."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
