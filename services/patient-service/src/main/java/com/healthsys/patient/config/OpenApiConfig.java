package com.healthsys.patient.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Documentação Swagger: http://localhost:8082/swagger-ui.html (use o token obtido no login do auth-service). */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "HealthSys - Patient Service",
                version = "0.1.0",
                description = "Gestão cadastral de pacientes."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
