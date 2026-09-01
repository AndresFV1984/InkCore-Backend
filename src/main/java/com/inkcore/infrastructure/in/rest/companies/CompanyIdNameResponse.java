package com.inkcore.infrastructure.in.rest.companies;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CompanyIdNameResponse", description = "Empresa (solo id y nombre)")
public record CompanyIdNameResponse(
        @Schema(description = "ID de la empresa", example = "company-seed-001")
        String id,

        @Schema(description = "Nombre de la empresa", example = "InkCore S.A.S.")
        String name
) {
}
