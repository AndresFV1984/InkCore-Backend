package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.reports.ProfitabilityReportItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "ProfitabilityReportSuccessEnvelope", description = "Listado de rentabilidad real por orden")
public record ProfitabilityReportSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-24T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Órdenes ordenadas por margen real ascendente")
        List<ProfitabilityReportItem> data
) {
}
