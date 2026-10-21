package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.station.StationResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "StationCompleteOrderIfReadySuccessEnvelope", description = "Evaluación de cierre: COMPLETED si todas las etapas están cumplidas")
public record StationCompleteOrderIfReadySuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-10-20T16:00:00Z")
        Instant timestamp,
        @Schema(implementation = StationResponses.CompleteStationOrderIfReadyResponse.class)
        StationResponses.CompleteStationOrderIfReadyResponse data
) {
}
