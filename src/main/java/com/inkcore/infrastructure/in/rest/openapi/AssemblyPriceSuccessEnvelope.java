package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.assemblyprices.AssemblyPriceResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AssemblyPriceSuccessEnvelope", description = "Respuesta exitosa de un precio de montaje")
public record AssemblyPriceSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-08T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = AssemblyPriceResponse.class)
        AssemblyPriceResponse data
) {
}
