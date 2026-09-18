package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ClientAccountsReceivableSuccessEnvelope", description = "Cartera consolidada de un cliente con cxcNumber por OP")
public record ClientAccountsReceivableSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-08T15:00:00Z")
        Instant timestamp,
        @Schema(implementation = OrderResponses.ClientAccountsReceivableResponse.class)
        OrderResponses.ClientAccountsReceivableResponse data
) {
}
