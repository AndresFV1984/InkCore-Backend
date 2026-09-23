package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AccountsReceivableDetailSuccessEnvelope", description = "Detalle CxC/Abonos "
        + "(cxcNumber + abonosNumber ABN-n + odpNumber) con entregas ODP y liquidaciones ABN")
public record AccountsReceivableDetailSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-05T16:20:00Z")
        Instant timestamp,
        @Schema(implementation = OrderResponses.AccountsReceivableDetailResponse.class)
        OrderResponses.AccountsReceivableDetailResponse data
) {
}
