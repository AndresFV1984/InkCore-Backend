package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.assemblyprices.AssemblyPriceResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AssemblyPriceListSuccessEnvelope", description = "Respuesta exitosa del listado paginado de precios de montaje")
public record AssemblyPriceListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-08T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Página de precios de montaje")
        PageResponse<AssemblyPriceResponse> data
) {
}
