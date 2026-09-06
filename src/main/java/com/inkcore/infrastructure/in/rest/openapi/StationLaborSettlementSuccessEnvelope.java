package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.application.station.usecase.GetStationLaborSettlementUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "StationLaborSettlementSuccessEnvelope", description = "Liquidación de tiempo laboral del operario")
public record StationLaborSettlementSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-01T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = GetStationLaborSettlementUseCase.LaborSettlement.class)
        GetStationLaborSettlementUseCase.LaborSettlement data
) {
}
