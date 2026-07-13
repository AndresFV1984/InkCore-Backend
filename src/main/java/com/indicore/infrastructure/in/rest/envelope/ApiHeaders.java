package com.indicore.infrastructure.in.rest.envelope;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cabeceras de metadatos del sobre de respuesta")
public record ApiHeaders(
        @Schema(description = "Identificador de correlacion (header X-Correlation-Id o generado)")
        String correlationId,
        @Schema(description = "Codigo de estado HTTP de la respuesta", example = "200")
        int status
) {
}