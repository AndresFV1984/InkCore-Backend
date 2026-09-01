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

/**
 * OpenAPI / Swagger UI.
 * <p>
 * {@code Info.description} va vacío (el detalle está en cada {@code @Operation}).
 * Los {@code Tag} llevan una descripción corta de una línea junto al nombre del grupo.
 */
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
                        .description("")
                        .version("0.0.41")
                        .contact(new Contact().name("InkCore").email("admin@indicolors.com")))
                .servers(List.of(
                        new Server().url(basePath).description("Context path local")
                ))
                .tags(List.of(
                        new Tag().name("Autenticación").description("Login y refresh de tokens"),
                        new Tag().name("Usuarios").description("Gestión de usuarios"),
                        new Tag().name("Clientes").description("Gestión de clientes"),
                        new Tag().name("Proveedores").description("Gestión de proveedores"),
                        new Tag().name("Empresas").description("Catálogo de empresas"),
                        new Tag().name("Vendedores").description("Gestión de vendedores"),
                        new Tag().name("Cuentas bancarias").description("Gestión de cuentas bancarias"),
                        new Tag().name("Terminados").description("Gestión de terminados"),
                        new Tag().name("Acabados").description("Gestión de acabados"),
                        new Tag().name("Tipos de papel").description("Catálogo de tipos de papel"),
                        new Tag().name("Despieces").description("Catálogo de despieces / patrones de corte"),
                        new Tag().name("Tipos de plancha").description("Catálogo de tipos de plancha"),
                        new Tag().name("Precios de montaje").description("Catálogo de precios de montaje"),
                        new Tag().name("Tarifas por millar").description("Catálogo de tarifas por millar"),
                        new Tag().name("Órdenes de producción").description(
                                "Wizard OP: especificaciones, preprensa, corte, impresión, terminados, acabados y cobro"),
                        new Tag().name("Conversión de color").description("Conversión RGB→CMYK (ICC / LittleCMS)"),
                        new Tag().name("Estimación de tinta").description("Estimación de consumo de tinta CMYK/spot"),
                        new Tag().name("Archivos estimación tinta").description(
                                "Presign PUT y URLs firmadas para inkEstimation en OP"),
                        new Tag().name("Roles").description("Catálogo de roles"),
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
