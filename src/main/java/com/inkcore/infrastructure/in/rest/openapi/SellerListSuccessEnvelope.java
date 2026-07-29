package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.sellers.SellerResponse;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Envelope concreto para documentar GET /api/v1/sellers/list en OpenAPI/Swagger. */
@Schema(name = "SellerListSuccessEnvelope", description = "Respuesta exitosa del listado paginado de vendedores")
public record SellerListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-07-25T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Página de vendedores")
        PageResponse<SellerResponse> data
) {
}
