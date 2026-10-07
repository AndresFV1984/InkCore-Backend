package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.papers.PaperNestedResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "PaperPriceListSuccessEnvelope", description = "Lista de precios vigentes del papel")
public record PaperPriceListSuccessEnvelope(
        ApiHeaders headers,
        Instant timestamp,
        @Schema(description = "Precios vigentes (incluye landedCostPerSheet)")
        List<PaperNestedResponses.PaperPriceResponse> data
) {
}
