package com.inkcore.infrastructure.in.rest.inkestimateassets;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InkEstimateAssetSignedUrlResponse")
public record InkEstimateAssetSignedUrlResponse(
        @Schema(description = "URL firmada de lectura (bucket privado)")
        String url,
        @Schema(description = "Segundos hasta expiración", example = "300")
        long expiresInSeconds
) {
}
