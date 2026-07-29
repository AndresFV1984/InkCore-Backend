package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.bankaccounts.BankAccountResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Envelope concreto para documentar create/get/update de cuentas bancarias en OpenAPI/Swagger. */
@Schema(name = "BankAccountSuccessEnvelope", description = "Respuesta exitosa de una cuenta bancaria")
public record BankAccountSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-07-28T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = BankAccountResponse.class)
        BankAccountResponse data
) {
}
