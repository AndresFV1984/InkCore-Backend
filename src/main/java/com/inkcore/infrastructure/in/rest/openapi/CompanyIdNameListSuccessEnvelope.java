package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.companies.CompanyIdNameResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/** Envelope concreto para documentar GET /api/v1/companies/ids-names en OpenAPI/Swagger. */
@Schema(
        name = "CompanyIdNameListSuccessEnvelope",
        description = "Respuesta exitosa del catálogo de empresas (id y nombre)"
)
public record CompanyIdNameListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-27T18:00:00Z")
        Instant timestamp,
        @ArraySchema(
                arraySchema = @Schema(description = "Empresas registradas"),
                schema = @Schema(implementation = CompanyIdNameResponse.class)
        )
        List<CompanyIdNameResponse> data
) {
}
