package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.station.StationResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "StationBitacoraSuccessEnvelope", description = "Bitácora resumida de un proceso/ítem")
public record StationBitacoraSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-01T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = StationResponses.BitacoraResponse.class)
        StationResponses.BitacoraResponse data
) {
}
