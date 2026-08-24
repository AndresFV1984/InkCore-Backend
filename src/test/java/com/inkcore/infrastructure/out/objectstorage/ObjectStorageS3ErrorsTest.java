package com.inkcore.infrastructure.out.objectstorage;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectStorageS3ErrorsTest {

    @Test
    void timeoutAndBrokenStreamAreRetryable() {
        assertTrue(ObjectStorageS3Errors.isRetryable(SdkClientException.create("timeout")));
        assertTrue(ObjectStorageS3Errors.isRetryable(new IllegalStateException("download", new IOException("reset"))));
        assertTrue(ObjectStorageS3Errors.isRetryable(NoSuchBucketException.builder().message("gone").build()));
        assertTrue(ObjectStorageS3Errors.isRetryable(S3Exception.builder()
                .statusCode(503)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("SlowDown").build())
                .message("slow")
                .build()));
    }

    @Test
    void missingObjectAndSizeLimitAreNotRetryable() {
        assertFalse(ObjectStorageS3Errors.isRetryable(NoSuchKeyException.builder().message("missing").build()));
        assertFalse(ObjectStorageS3Errors.isRetryable(new IllegalArgumentException("Objeto no encontrado")));
        assertFalse(ObjectStorageS3Errors.isRetryable(new IllegalArgumentException("Objeto supera el tamaño máximo permitido")));
        assertFalse(ObjectStorageS3Errors.isRetryable(S3Exception.builder()
                .statusCode(403)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").build())
                .message("denied")
                .build()));
    }

    @Test
    void noSuchKeyIsNotFound_noSuchBucketIsTransient() {
        assertTrue(ObjectStorageS3Errors.isNotFound(NoSuchKeyException.builder().message("missing").build()));
        assertFalse(ObjectStorageS3Errors.isNotFound(NoSuchBucketException.builder().message("bucket").build()));
        assertTrue(ObjectStorageS3Errors.isTransient(NoSuchBucketException.builder().message("bucket").build()));
    }
}
