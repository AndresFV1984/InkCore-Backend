package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.productionorders.ProductionOrderResponse;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ProductionOrderListSuccessEnvelope", description = "Listado paginado de órdenes de producción")
public record ProductionOrderListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-04T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Página de órdenes")
        PageResponse<ProductionOrderResponse> data
) {
}
