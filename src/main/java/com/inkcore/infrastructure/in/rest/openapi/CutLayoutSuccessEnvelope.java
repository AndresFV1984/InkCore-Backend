package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.cutlayouts.CutLayoutResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "CutLayoutSuccessEnvelope", description = "Respuesta exitosa de un despiece")
public record CutLayoutSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-01T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = CutLayoutResponse.class)
        CutLayoutResponse data
) {
}
