package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.clients.ClientResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Envelope concreto para documentar GET /api/v1/clients/list en OpenAPI/Swagger. */
@Schema(name = "ClientListSuccessEnvelope", description = "Respuesta exitosa del listado paginado de clientes")
public record ClientListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-07-22T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Página de clientes")
        PageResponse<ClientResponse> data
) {
}
