package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import com.inkcore.infrastructure.in.rest.thousandrates.ThousandRateResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ThousandRateListSuccessEnvelope", description = "Respuesta exitosa del listado paginado de tarifas por millar")
public record ThousandRateListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-08T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Página de tarifas por millar", implementation = PageResponse.class)
        PageResponse<ThousandRateResponse> data
) {
}
