package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "OrderApplyCreditToCxcSuccessEnvelope",
        description = "Saldo a favor del pedido aplicado a una CxC (applications + accountsReceivable)")
public record OrderApplyCreditToCxcSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-06T10:00:00Z")
        Instant timestamp,
        @Schema(implementation = OrderResponses.ApplyCreditToCxcResponse.class)
        OrderResponses.ApplyCreditToCxcResponse data
) {
}
