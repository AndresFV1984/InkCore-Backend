package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.bankaccounts.BankAccountResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Envelope concreto para documentar GET /api/v1/bank-accounts/list en OpenAPI/Swagger. */
@Schema(name = "BankAccountListSuccessEnvelope", description = "Respuesta exitosa del listado paginado de cuentas bancarias")
public record BankAccountListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-07-28T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Página de cuentas bancarias")
        PageResponse<BankAccountResponse> data
) {
}
