package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import com.inkcore.infrastructure.in.rest.station.StationResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "StationInboxListSuccessEnvelope", description = "Inbox paginado de órdenes asignadas al operario")
public record StationInboxListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-01T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = PageResponse.class)
        PageResponse<StationResponses.InboxItemResponse> data
) {
}
