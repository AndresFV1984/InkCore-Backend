package com.indicore.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI indicoreOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("IndiCore API")
                        .description("""
                                REST con arquitectura hexagonal. Rutas: `/api/v1/{recurso}/{accion}` (ver docs/CONVENCION_ENDPOINTS.md).
                                Autenticación JWT (claim `tv` + `roles`). Esquema de BD: `indicolors`.""")
                        .version("0.0.1"))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Obtener token en POST /api/v1/auth/login")));
    }
}
