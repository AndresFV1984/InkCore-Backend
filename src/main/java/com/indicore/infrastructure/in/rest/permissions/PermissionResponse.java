package com.indicore.infrastructure.in.rest.permissions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "PermissionResponse", description = "Permiso del catálogo (checkbox del formulario)")
public record PermissionResponse(
        @Schema(description = "Identificador del permiso", example = "c1000001-0000-4000-8000-000000000008")
        UUID permissionId,

        @Schema(description = "Código del permiso (usar en register/update como permissionCodes)", example = "VER_ORDENES_PRODUCCION")
        String code,

        @Schema(description = "Etiqueta visible en el formulario", example = "Ver órdenes de producción")
        String name
) {
}
