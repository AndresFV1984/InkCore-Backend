package com.inkcore.infrastructure.out.objectstorage;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

/**
 * Distingue 404/403 (no reintentar) de timeouts, 5xx y bucket ausente (reintentar → 503).
 */
final class ObjectStorageS3Errors {

    private ObjectStorageS3Errors() {
    }

    static boolean isRetryable(Throwable ex) {
        if (ex instanceof IllegalArgumentException) {
            return false;
        }
        return isTransient(ex);
    }

    static boolean isNotFound(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof NoSuchKeyException) {
                return true;
            }
            if (t instanceof S3Exception s3) {
                String code = errorCode(s3);
                if ("NoSuchKey".equals(code) || "NotFound".equals(code)) {
                    return true;
                }
                if (s3.statusCode() == 404 && !"NoSuchBucket".equals(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean isForbidden(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof S3Exception s3) {
                if (s3.statusCode() == 403 || "AccessDenied".equals(errorCode(s3))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean isTransient(Throwable ex) {
        if (isNotFound(ex) || isForbidden(ex)) {
            return false;
        }
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof NoSuchBucketException) {
                return true;
            }
            if (t instanceof SdkClientException) {
                return true;
            }
            if (t instanceof IOException) {
                return true;
            }
            if (t instanceof S3Exception s3) {
                int status = s3.statusCode();
                if (status == 429 || status == 500 || status == 502 || status == 503 || status == 504) {
                    return true;
                }
                String code = errorCode(s3);
                if ("SlowDown".equals(code)
                        || "RequestTimeout".equals(code)
                        || "ServiceUnavailable".equals(code)
                        || "InternalError".equals(code)
                        || "NoSuchBucket".equals(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String errorCode(S3Exception s3) {
        if (s3.awsErrorDetails() == null || s3.awsErrorDetails().errorCode() == null) {
            return "";
        }
        return s3.awsErrorDetails().errorCode();
    }
}
