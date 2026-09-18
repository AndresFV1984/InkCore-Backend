package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AccountsReceivableListSuccessEnvelope", description = "Listado paginado CxC con accountsReceivableId y cxcNumber")
public record AccountsReceivableListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-05T16:15:00Z")
        Instant timestamp,
        @Schema(implementation = PageResponse.class)
        PageResponse<OrderResponses.AccountsReceivableItemResponse> data
) {
}
