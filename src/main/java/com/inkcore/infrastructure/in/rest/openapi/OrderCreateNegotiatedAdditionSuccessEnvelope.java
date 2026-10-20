package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "OrderCreateNegotiatedAdditionSuccessEnvelope", description = "Adicional ADN-{n} "
        + "con amountToCollect y accountsReceivable (totalOwed incluye el adicional vigente)")
public record OrderCreateNegotiatedAdditionSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-10-19T16:00:01Z")
        Instant timestamp,
        @Schema(implementation = OrderResponses.CreateNegotiatedAdditionResponse.class)
        OrderResponses.CreateNegotiatedAdditionResponse data
) {
}
