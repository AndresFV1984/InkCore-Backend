package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.suppliers.SupplierResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Envelope concreto para documentar create/get/update de proveedores en OpenAPI/Swagger. */
@Schema(name = "SupplierSuccessEnvelope", description = "Respuesta exitosa de un proveedor")
public record SupplierSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-27T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = SupplierResponse.class)
        SupplierResponse data
) {
}
