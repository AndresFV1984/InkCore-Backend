package com.inkcore.infrastructure.in.rest.envelope;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cabeceras de metadatos del sobre de respuesta (tokens solo en login/refresh)")
public record ApiHeaders(
        @Schema(description = "Identificador de correlación (header X-Correlation-Id o generado)")
        String correlationId,

        @Schema(description = "Código de estado HTTP de la respuesta", example = "200")
        Integer status,

        @Schema(description = "Access token con prefijo Bearer", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "TTL del access token en segundos", example = "3600")
        Long tokenAccesExpira,

        @Schema(description = "Refresh token opaco", example = "dGhpc2lzYXJlZnJlc2h0b2tlbg")
        String refreshToken,

        @Schema(description = "TTL del refresh token en segundos", example = "1209600")
        Long tokenRefresExpira,

        @Schema(description = "Código HTTP de la operación", example = "200")
        Integer statusCode,

        @Schema(description = "Código de resultado de negocio", example = "OK")
        String code,

        @Schema(description = "Descripción del resultado", example = "Login successful")
        String description
) {
    public static ApiHeaders basic(String correlationId, int httpStatus) {
        return new ApiHeaders(correlationId, httpStatus, null, null, null, null, null, null, null);
    }

    /** Headers de respuestas autenticadas estándar (sin token ni status). */
    public static ApiHeaders standard(String correlationId) {
        return new ApiHeaders(
                correlationId,
                null,
                null,
                null,
                null,
                null,
                200,
                "OK",
                "Success"
        );
    }

    /** Headers de creación (201); sin token. */
    public static ApiHeaders created(String correlationId, String code, String description) {
        return new ApiHeaders(
                correlationId,
                null,
                null,
                null,
                null,
                null,
                201,
                code,
                description
        );
    }

    public static ApiHeaders withTokens(
            String correlationId,
            String bearerAccessToken,
            long accessExpiresInSeconds,
            String refreshToken,
            long refreshExpiresInSeconds,
            String code,
            String description
    ) {
        return new ApiHeaders(
                correlationId,
                200,
                bearerAccessToken,
                accessExpiresInSeconds,
                refreshToken,
                refreshExpiresInSeconds,
                200,
                code,
                description
        );
    }
}
