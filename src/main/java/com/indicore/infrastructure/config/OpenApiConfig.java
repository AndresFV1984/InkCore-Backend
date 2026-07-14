package com.indicore.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI indicoreOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("InkCore API")
                        .description("""
                                Backend REST InkCore (arquitectura hexagonal).

                                ## Autenticación
                                1. `POST /api/v1/auth/login` con correo y contraseña.
                                2. Usar el `accessToken` en **Authorize** (Bearer JWT).
                                3. Claim `tv` valida la versión de sesión; claim `roles` define autorización.

                                ## Convención de rutas
                                `/api/v1/{recurso}/{accion}` — ver `docs/CONVENCION_ENDPOINTS.md`.

                                ## Usuarios (formulario frontend)
                                - Registrar: `POST /api/v1/users/register`
                                - Actualizar: `PUT /api/v1/users/update/{userId}`
                                - Listar: `GET /api/v1/users/list`
                                - Catálogos: `/api/v1/roles/list` y `/api/v1/permissions/list`

                                Esquema de BD: `indicolors`.
                                """)
                        .version("0.0.1")
                        .contact(new Contact().name("InkCore").email("admin@indicolors.com")))
                .tags(List.of(
                        new Tag().name("Autenticación").description("Login y emisión de JWT"),
                        new Tag().name("Usuarios").description("Registro, actualización, listado y consulta"),
                        new Tag().name("Roles").description("Catálogo para el dropdown de rol"),
                        new Tag().name("Permisos").description("Catálogo para checkboxes de permisos")
                ))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Pegar el accessToken obtenido en POST /api/v1/auth/login")));
    }
}
