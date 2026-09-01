package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import com.inkcore.infrastructure.in.rest.suppliers.SupplierResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Envelope concreto para documentar GET /api/v1/suppliers/list en OpenAPI/Swagger. */
@Schema(name = "SupplierListSuccessEnvelope", description = "Respuesta exitosa del listado paginado de proveedores")
public record SupplierListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-27T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Página de proveedores")
        PageResponse<SupplierResponse> data
) {
}
