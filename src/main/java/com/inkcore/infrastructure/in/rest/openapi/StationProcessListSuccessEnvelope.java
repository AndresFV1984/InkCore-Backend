package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.station.StationResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "StationProcessListSuccessEnvelope", description = "Procesos de una OP para estación")
public record StationProcessListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-01T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = StationResponses.ProcessRowResponse.class)
        List<StationResponses.ProcessRowResponse> data
) {
}
