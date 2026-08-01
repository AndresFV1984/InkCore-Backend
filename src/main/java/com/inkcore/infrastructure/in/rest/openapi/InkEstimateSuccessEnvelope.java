package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.inkestimates.InkEstimateResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
        name = "InkEstimateSuccessEnvelope",
        description = "Envelope de éxito para POST /api/v1/ink-estimates/estimate"
)
public record InkEstimateSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta (correlationId, statusCode, code)") ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC") Instant timestamp,
        @Schema(implementation = InkEstimateResponse.class, description = "Datos de cobertura y gramos")
        InkEstimateResponse data
) {
}
