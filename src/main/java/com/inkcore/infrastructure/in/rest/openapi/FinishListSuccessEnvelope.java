package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.finishes.FinishResponse;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "FinishedProductListSuccessEnvelope", description = "Respuesta exitosa del listado paginado de productos terminados")
public record FinishListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-07-28T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Página de productos terminados")
        PageResponse<FinishResponse> data
) {
}
