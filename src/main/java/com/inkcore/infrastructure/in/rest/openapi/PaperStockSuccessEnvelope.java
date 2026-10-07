package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.papers.PaperNestedResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "PaperStockSuccessEnvelope", description = "Lote de inventario de papel")
public record PaperStockSuccessEnvelope(
        ApiHeaders headers,
        Instant timestamp,
        PaperNestedResponses.PaperStockResponse data
) {
}
