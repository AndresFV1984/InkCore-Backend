package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.cutlayouts.CutLayoutResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "CutLayoutListSuccessEnvelope", description = "Respuesta exitosa del listado paginado de despieces")
public record CutLayoutListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-01T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Página de despieces")
        PageResponse<CutLayoutResponse> data
) {
}
