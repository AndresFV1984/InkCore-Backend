package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.platetypes.PlateTypeResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "PlateTypeSuccessEnvelope", description = "Respuesta exitosa de un tipo de plancha")
public record PlateTypeSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-04T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = PlateTypeResponse.class)
        PlateTypeResponse data
) {
}
