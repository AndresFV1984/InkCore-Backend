package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.clients.ClientResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Envelope concreto para documentar create/get/update de clientes en OpenAPI/Swagger. */
@Schema(name = "ClientSuccessEnvelope", description = "Respuesta exitosa de un cliente")
public record ClientSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-07-22T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = ClientResponse.class)
        ClientResponse data
) {
}
