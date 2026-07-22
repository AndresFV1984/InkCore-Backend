package com.inkcore.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/InkCore-backend}")
    private String contextPath;

    @Bean
    OpenAPI inkCoreOpenApi() {
        final String scheme = "bearerAuth";
        String basePath = contextPath == null || contextPath.isBlank() ? "" : contextPath;
        return new OpenAPI()
                .info(new Info()
                        .title("InkCore API")
                        .description("Backend REST InkCore.")
                        .version("0.0.5")
                        .contact(new Contact().name("InkCore").email("admin@indicolors.com")))
                .servers(List.of(
                        new Server().url(basePath).description("Context path local")
                ))
                .tags(List.of(
                        new Tag().name("Autenticación").description("Login y refresh de tokens"),
                        new Tag().name("Usuarios").description("Alta, login, listado, perfil y actualización"),
                        new Tag().name("Clientes").description(
                                "Alta, listado, consulta y actualización. JWT requerido. "
                                        + "Respuesta con documentType anidado { documentType, identificationNumber }."
                        ),
                        new Tag().name("Roles").description("Catálogo de roles (usar `code` o `name` en el campo `role`)"),
                        new Tag().name("Permisos").description("Catálogo de permisos")
                ))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        1) POST /api/v1/users/login
                                        2) Copiar headers.token (quitar el prefijo "Bearer ")
                                        3) Pegar aquí solo el JWT
                                        """)));
    }
}
