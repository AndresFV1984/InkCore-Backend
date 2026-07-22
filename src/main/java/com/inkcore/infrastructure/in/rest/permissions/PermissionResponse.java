package com.inkcore.infrastructure.in.rest.permissions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "PermissionResponse", description = "Permiso del catálogo (checkbox del formulario)")
public record PermissionResponse(
        @Schema(description = "Identificador del permiso", example = "c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a08")
        UUID permissionId,

        @Schema(description = "Código del permiso (usar en register/update como permissionCodes)", example = "production.orders.view")
        String code,

        @Schema(description = "Etiqueta visible en el formulario", example = "Ver órdenes de producción")
        String name
) {
}
