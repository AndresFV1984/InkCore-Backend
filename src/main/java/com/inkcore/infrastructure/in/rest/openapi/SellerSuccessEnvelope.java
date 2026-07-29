package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.sellers.SellerResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Envelope concreto para documentar create/get/update de vendedores en OpenAPI/Swagger. */
@Schema(name = "SellerSuccessEnvelope", description = "Respuesta exitosa de un vendedor")
public record SellerSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-07-25T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = SellerResponse.class)
        SellerResponse data
) {
}
