package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.station.StationResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "StationEventSuccessEnvelope", description = "Respuesta exitosa al registrar un evento de estación")
public record StationEventSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-01T15:22:10Z")
        Instant timestamp,
        @Schema(implementation = StationResponses.EventResponse.class)
        StationResponses.EventResponse data
) {
}
