package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.inkestimateassets.InkEstimateAssetSignedUrlResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "InkEstimateAssetSignedUrlSuccessEnvelope")
public record InkEstimateAssetSignedUrlSuccessEnvelope(
        ApiHeaders headers,
        Instant timestamp,
        InkEstimateAssetSignedUrlResponse data
) {
}
