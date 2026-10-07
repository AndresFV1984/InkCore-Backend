package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.papers.PaperNestedResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "PaperRemnantSuccessEnvelope", description = "Remanente reutilizable de papel")
public record PaperRemnantSuccessEnvelope(
        ApiHeaders headers,
        Instant timestamp,
        PaperNestedResponses.PaperRemnantResponse data
) {
}
