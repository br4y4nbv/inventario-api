package com.br4y4nbv.inventario_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventario API")
                        .version("1.0")
                        .description("""
                                API REST de inventario con autenticación JWT, roles y control de concurrencia.

                                **Cómo probarla:** registra un usuario o inicia sesión en `/api/auth/login`, \
                                copia el `accessToken`, pulsa **Authorize** y pégalo **sin** el prefijo "Bearer".
                                """))
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")))
                // Por defecto, todos los endpoints piden token; los públicos lo anulan con @SecurityRequirements
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}