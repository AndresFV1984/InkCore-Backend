package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "OrderAvailabilitySuccessEnvelope", description = "Disponibilidad comercial de una OP")
public record OrderAvailabilitySuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-05T14:20:00Z")
        Instant timestamp,
        @Schema(implementation = OrderResponses.AvailabilityResponse.class)
        OrderResponses.AvailabilityResponse data
) {
}
