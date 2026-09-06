package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "OrderDeliveryListSuccessEnvelope", description = "Listado de entregas de una OP")
public record OrderDeliveryListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-05T14:35:00Z")
        Instant timestamp,
        @Schema(implementation = OrderResponses.DeliveryResponse.class)
        List<OrderResponses.DeliveryResponse> data
) {
}
