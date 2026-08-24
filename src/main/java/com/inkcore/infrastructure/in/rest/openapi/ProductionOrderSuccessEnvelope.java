package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ProductionOrderSuccessEnvelope", description = "Respuesta exitosa de una orden de producción")
public record ProductionOrderSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-04T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = ProductionOrderResponse.class)
        ProductionOrderResponse data
) {
}
