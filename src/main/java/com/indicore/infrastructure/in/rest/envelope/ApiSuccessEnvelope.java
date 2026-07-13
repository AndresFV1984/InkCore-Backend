package com.indicore.infrastructure.in.rest.envelope;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Respuesta exitosa. Solo incluye headers, timestamp y data.")
public record ApiSuccessEnvelope<T>(
        @Schema(description = "Metadatos de la respuesta (correlationId y codigo HTTP)")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC de la respuesta", example = "2026-05-17T16:00:00Z")
        Instant timestamp,
        @Schema(description = "Cuerpo util de la operacion")
        T data
) {
}