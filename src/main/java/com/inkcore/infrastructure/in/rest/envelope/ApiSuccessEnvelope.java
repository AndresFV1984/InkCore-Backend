package com.inkcore.infrastructure.in.rest.envelope;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Respuesta exitosa. Solo incluye headers, timestamp y data.")
public record ApiSuccessEnvelope<T>(
        @Schema(description = "Metadatos de la respuesta (correlationId y codigo HTTP)")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC de la respuesta", example = "2026-05-17T16:00:00Z")
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Schema(description = "Cuerpo útil de la operación (null en refresh)", nullable = true)
        T data
) {
}
