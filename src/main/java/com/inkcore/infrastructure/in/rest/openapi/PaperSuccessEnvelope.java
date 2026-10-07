package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.papers.PaperResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "PaperSuccessEnvelope", description = "Respuesta exitosa de un papel")
public record PaperSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-04T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = PaperResponse.class)
        PaperResponse data
) {
}
