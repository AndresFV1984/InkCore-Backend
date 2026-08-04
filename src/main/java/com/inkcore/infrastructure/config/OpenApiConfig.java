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
                        .description("""
                                Backend REST InkCore.
                                Incluye autenticación, usuarios, clientes, vendedores,
                                cuentas bancarias, productos terminados, acabados, roles, permisos,
                                conversión de color RGB→CMYK (LittleCMS + ICC; TIFF/PDF) y estimación
                                de consumo de tinta (PDFBox + LittleCMS; CMYK/spot nativos; sin RIP de pago
                                ni Ghostscript AGPL; sin base Pantone de pago).
                                El CMM LittleCMS (lcms2) viaja empaquetado en el JAR; no requiere instalación
                                manual en el host.
                                Listados paginados (`page` 0-based, `size` default 20 / máx 100):
                                data = { content, page, size, totalElements, totalPages, hasNext }.
                                """)
                        .version("0.0.26")
                        .contact(new Contact().name("InkCore").email("admin@indicolors.com")))
                .servers(List.of(
                        new Server().url(basePath).description("Context path local")
                ))
                .tags(List.of(
                        new Tag().name("Autenticación").description("Login y refresh de tokens"),
                        new Tag().name("Usuarios").description("Gestión de usuarios"),
                        new Tag().name("Clientes").description("Gestión de clientes"),
                        new Tag().name("Vendedores").description("Gestión de vendedores"),
                        new Tag().name("Cuentas bancarias").description("Gestión de cuentas bancarias"),
                        new Tag().name("Terminados").description("Gestión de terminados"),
                        new Tag().name("Acabados").description("Gestión de acabados"),
                        new Tag().name("Conversión de color").description(
                                "RGB→CMYK con LittleCMS (CMM nativo + BPC) e ICC. "
                                        + "Salida TIFF/PDF. Soft-proof JPEG para UI. "
                                        + "GET /color-conversions/list para perfiles disponibles."
                        ),
                        new Tag().name("Estimación de tinta").description(
                                "POST /ink-estimates/estimate — cobertura CMYK + spots; RGB→CMYK con LittleCMS "
                                        + "obligatorio (colorEngine=littlecms). pages opcional (1-based, máx. 2). "
                                        + "Factores g/cm² por canal. Sin RIP de pago."
                        ),
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
