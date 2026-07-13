package com.indicore.infrastructure.in.rest.openapi;

import com.indicore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado o token inválido",
                content = @Content(schema = @Schema(implementation = ApiErrorEnvelope.class))
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Sin permiso para el recurso",
                content = @Content(schema = @Schema(implementation = ApiErrorEnvelope.class))
        ),
        @ApiResponse(
                responseCode = "404",
                description = "Recurso no encontrado",
                content = @Content(schema = @Schema(implementation = ApiErrorEnvelope.class))
        ),
        @ApiResponse(
                responseCode = "409",
                description = "Conflicto (recurso duplicado)",
                content = @Content(schema = @Schema(implementation = ApiErrorEnvelope.class))
        )
})
public @interface ApiSecuredErrorResponses {
}
