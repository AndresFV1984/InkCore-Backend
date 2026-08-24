package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.thousandrates.ThousandRateResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ThousandRateSuccessEnvelope", description = "Respuesta exitosa de una tarifa por millar")
public record ThousandRateSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-08T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = ThousandRateResponse.class)
        ThousandRateResponse data
) {
}
