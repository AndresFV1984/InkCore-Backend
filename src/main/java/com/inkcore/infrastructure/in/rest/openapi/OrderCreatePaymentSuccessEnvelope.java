package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "OrderCreatePaymentSuccessEnvelope", description = "Abono/reversión con paymentNumber ABN-{n} y accountsReceivable (CXC)")
public record OrderCreatePaymentSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-05T16:00:01Z")
        Instant timestamp,
        @Schema(implementation = OrderResponses.CreatePaymentResponse.class)
        OrderResponses.CreatePaymentResponse data
) {
}
