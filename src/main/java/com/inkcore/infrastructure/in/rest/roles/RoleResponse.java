package com.inkcore.infrastructure.in.rest.roles;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "RoleResponse", description = "Rol del catálogo (por empresa)")
public record RoleResponse(
        @Schema(description = "Identificador del rol", example = "b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22")
        UUID roleId,

        @Schema(description = "Empresa dueña del rol", example = "company-seed-001")
        String companyId,

        @Schema(description = "Código de autorización (JWT)", example = "ADMINISTRADOR")
        String code,

        @Schema(description = "Nombre visible", example = "Operador")
        String name,

        @Schema(description = "Descripción", example = "Rol operativo de planta de producción")
        String description,

        @Schema(description = "true = Activo", example = "true")
        boolean state,

        @ArraySchema(
                arraySchema = @Schema(description = "Permisos del rol"),
                schema = @Schema(example = "production.orders.view")
        )
        List<String> permissionCodes
) {
}
