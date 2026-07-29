package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.finishes.FinishResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "FinishedProductSuccessEnvelope", description = "Respuesta exitosa de un producto terminado")
public record FinishSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-07-28T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = FinishResponse.class)
        FinishResponse data
) {
}
