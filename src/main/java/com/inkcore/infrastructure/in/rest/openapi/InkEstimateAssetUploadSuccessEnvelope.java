package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.inkestimateassets.InkEstimateAssetSignedUrlResponse;
import com.inkcore.infrastructure.in.rest.inkestimateassets.InkEstimateAssetUploadResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "InkEstimateAssetUploadSuccessEnvelope")
public record InkEstimateAssetUploadSuccessEnvelope(
        ApiHeaders headers,
        Instant timestamp,
        InkEstimateAssetUploadResponse data
) {
}
