package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.papertypes.PaperTypeResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "PaperTypeSuccessEnvelope", description = "Respuesta exitosa de un tipo de papel")
public record PaperTypeSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-01T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = PaperTypeResponse.class)
        PaperTypeResponse data
) {
}
