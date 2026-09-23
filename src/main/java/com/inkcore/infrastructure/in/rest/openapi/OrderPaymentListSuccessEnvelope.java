package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "OrderPaymentListSuccessEnvelope", description = "Listado de liquidaciones/reversiones "
        + "con paymentNumber ABN-{n} (≠ abonosNumber del agregado; secuencia ABN compartida)")
public record OrderPaymentListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-05T16:10:00Z")
        Instant timestamp,
        @Schema(implementation = OrderResponses.PaymentResponse.class)
        List<OrderResponses.PaymentResponse> data
) {
}
