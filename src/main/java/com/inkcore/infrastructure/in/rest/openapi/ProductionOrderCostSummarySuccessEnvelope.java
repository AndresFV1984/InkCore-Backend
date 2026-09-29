package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.productionorders.CostSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ProductionOrderCostSummarySuccessEnvelope", description = "Resumen de costo estimado contra real de una orden")
public record ProductionOrderCostSummarySuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-24T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = CostSummaryResponse.class)
        CostSummaryResponse data
) {
}
