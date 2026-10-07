package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.papers.PaperNestedResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "PaperRemnantListSuccessEnvelope", description = "Listado de remanentes reutilizables del papel")
public record PaperRemnantListSuccessEnvelope(
        ApiHeaders headers,
        Instant timestamp,
        List<PaperNestedResponses.PaperRemnantResponse> data
) {
}
