package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.papers.PaperNestedResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "PaperCutLayoutSuccessEnvelope", description = "Despiece asociado a un papel")
public record PaperCutLayoutSuccessEnvelope(
        ApiHeaders headers,
        Instant timestamp,
        PaperNestedResponses.PaperCutLayoutResponse data
) {
}
