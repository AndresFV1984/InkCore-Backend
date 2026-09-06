package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.application.station.usecase.GetStationTraceReportUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "StationTraceReportSuccessEnvelope", description = "Reporte de trazabilidad de estación")
public record StationTraceReportSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-01T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = GetStationTraceReportUseCase.TraceReport.class)
        GetStationTraceReportUseCase.TraceReport data
) {
}
