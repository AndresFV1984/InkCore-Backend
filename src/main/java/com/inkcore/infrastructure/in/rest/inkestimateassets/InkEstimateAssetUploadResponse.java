package com.inkcore.infrastructure.in.rest.inkestimateassets;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(name = "InkEstimateAssetUploadResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InkEstimateAssetUploadResponse(
        @Schema(description = "Clave del arte original en object storage", example = "tmp/company/company-seed-001/ink-estimates/user-1/entry-1/original.pdf")
        String objectKey,
        @Schema(description = "Clave de la miniatura JPEG", example = "tmp/company/company-seed-001/ink-estimates/user-1/entry-1/preview.jpg")
        String previewObjectKey,
        @Schema(example = "application/pdf")
        String contentType,
        @Schema(example = "1048576")
        long sizeBytes,
        @Schema(example = "arte-brochure.pdf")
        String fileName,
        @Schema(description = "PUT prefirmado del original. El navegador sube directo a S3, sin JWT.")
        InkEstimatePresignedPartResponse upload,
        @Schema(description = "PUT prefirmado de la miniatura JPEG, si se pidió.")
        InkEstimatePresignedPartResponse previewUpload
) {
    @Schema(name = "InkEstimatePresignedPartResponse")
    public record InkEstimatePresignedPartResponse(
            @Schema(example = "https://minio.example/inkcore/key?X-Amz-Algorithm=AWS4-HMAC-SHA256")
            String url,
            @Schema(allowableValues = {"PUT"}, example = "PUT")
            String method,
            @Schema(description = "Cabeceras firmadas que el front debe reenviar tal cual")
            Map<String, String> headers,
            @Schema(example = "300")
            long expiresInSeconds
    ) {
    }
}
