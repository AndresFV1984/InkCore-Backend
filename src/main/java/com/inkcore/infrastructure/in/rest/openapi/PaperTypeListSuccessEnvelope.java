package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.papertypes.PaperTypeResponse;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "PaperTypeListSuccessEnvelope", description = "Respuesta exitosa del listado paginado de tipos de papel")
public record PaperTypeListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-01T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Página de tipos de papel")
        PageResponse<PaperTypeResponse> data
) {
}
