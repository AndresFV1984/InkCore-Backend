package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "CxcDocumentDashboardListSuccessEnvelope",
        description = "Listado paginado de documentos CxC (1 fila = ENT o ADN)")
public record CxcDocumentDashboardListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-05T16:15:00Z")
        Instant timestamp,
        @Schema(implementation = PageResponse.class)
        PageResponse<OrderResponses.CxcDocumentDashboardItemResponse> data
) {
}
