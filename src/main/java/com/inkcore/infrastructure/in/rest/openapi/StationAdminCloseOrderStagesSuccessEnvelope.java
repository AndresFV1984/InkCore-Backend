package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.station.StationResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "StationAdminCloseOrderStagesSuccessEnvelope", description = "Cierre administrativo de etapas incompletas (solo ADMINISTRADOR)")
public record StationAdminCloseOrderStagesSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-10-20T16:05:00Z")
        Instant timestamp,
        @Schema(implementation = StationResponses.AdminCloseOrderStagesResponse.class)
        StationResponses.AdminCloseOrderStagesResponse data
) {
}
