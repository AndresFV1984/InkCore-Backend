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
                responseCode = "400",
                description = "Solicitud inválida",
                content = @Content(schema = @Schema(implementation = ApiErrorEnvelope.class))
        ),
        @ApiResponse(
                responseCode = "422",
                description = "Regla de negocio incumplida",
                content = @Content(schema = @Schema(implementation = ApiErrorEnvelope.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Error interno",
                content = @Content(schema = @Schema(implementation = ApiErrorEnvelope.class))
        )
})
public @interface ApiErrorResponses {
}
