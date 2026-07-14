package com.indicore.infrastructure.in.rest.roles;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "RoleResponse", description = "Rol disponible en el catálogo del sistema")
public record RoleResponse(
        @Schema(description = "Identificador del rol", example = "b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22")
        UUID roleId,

        @Schema(description = "Código del rol (usar en register/update como roleCode)", example = "OPERADOR")
        String code,

        @Schema(description = "Nombre visible en el formulario", example = "Operador")
        String name
) {
}
