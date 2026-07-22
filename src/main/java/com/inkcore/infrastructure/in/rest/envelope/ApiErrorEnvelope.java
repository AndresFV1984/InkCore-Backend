package com.inkcore.infrastructure.in.rest.envelope;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de error. Incluye path, message y lista opcional de errors.")
public record ApiErrorEnvelope(
        @Schema(description = "Metadatos de la respuesta (correlationId y codigo HTTP)")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC de la respuesta", example = "2026-05-17T16:00:00Z")
        Instant timestamp,
        @Schema(description = "Ruta HTTP que origino el error", example = "/InkCore-backend/api/v1/users/register")
        String path,
        @Schema(description = "Mensaje resumido del error", example = "Datos invalidos")
        String message,
        @Schema(description = "Detalle por campo o codigo de dominio")
        List<String> errors
) {
}