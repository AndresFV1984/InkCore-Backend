package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.users.UserListItemResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/** Envelope concreto para documentar GET /api/v1/users/list en OpenAPI/Swagger. */
@Schema(name = "UserListSuccessEnvelope", description = "Respuesta exitosa del listado de usuarios")
public record UserListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-07-18T06:00:00Z")
        Instant timestamp,
        @ArraySchema(
                arraySchema = @Schema(description = "Usuarios del sistema"),
                schema = @Schema(implementation = UserListItemResponse.class)
        )
        List<UserListItemResponse> data
) {
}
