package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ReprintWasteSuccessEnvelope", description = "Respuesta al registrar un retrabajo")
public record ReprintWasteSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-25T17:00:00Z")
        Instant timestamp,
        @Schema(implementation = ProductionOrderResponse.ReprintWasteResponse.class)
        ProductionOrderResponse.ReprintWasteResponse data
) {
}
