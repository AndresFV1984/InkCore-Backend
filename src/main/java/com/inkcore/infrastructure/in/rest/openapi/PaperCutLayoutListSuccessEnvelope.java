package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.papers.PaperNestedResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "PaperCutLayoutListSuccessEnvelope", description = "Listado de despieces asociados a un papel")
public record PaperCutLayoutListSuccessEnvelope(
        ApiHeaders headers,
        Instant timestamp,
        List<PaperNestedResponses.PaperCutLayoutResponse> data
) {
}
