package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "OrderCreateCxcFromAdditionSuccessEnvelope",
        description = "CxC concepto extra creada desde un ADN (cxcDocument + accountsReceivable ABN)")
public record OrderCreateCxcFromAdditionSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-10-19T16:00:02Z")
        Instant timestamp,
        @Schema(implementation = OrderResponses.CreateCxcFromAdditionResponse.class)
        OrderResponses.CreateCxcFromAdditionResponse data
) {
}
