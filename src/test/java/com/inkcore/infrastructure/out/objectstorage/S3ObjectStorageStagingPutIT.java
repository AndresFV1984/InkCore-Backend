package com.inkcore.infrastructure.out.objectstorage;

import com.inkcore.domain.objectstorage.model.PresignedUpload;
import com.inkcore.domain.productionorder.service.InkEstimateAssetKeyPolicy;
import com.inkcore.infrastructure.config.ObjectStorageProperties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.Tag;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Requiere MinIO en {@code localhost:9100} (docker compose). Si no está, se omite.
 */
class S3ObjectStorageStagingPutIT {

    @Test
    void presignedPut_persistsStagingTag_andRejectsPutWithoutSignedTaggingHeader() throws Exception {
        assumeTrue(minioReachable(), "MinIO no está en localhost:9100");

        ObjectStorageProperties properties = minioProperties();
        S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(properties);
        String objectKey = "tmp/company/it/ink-estimates/u1/" + UUID.randomUUID() + "/original.pdf";
        PresignedUpload upload = adapter.createPresignedPutUrl(
                objectKey,
                "application/pdf",
                Duration.ofSeconds(300),
                Map.of("company-id", "it", "user-id", "u1"),
                Map.of(
                        InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_KEY,
                        InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_VALUE
                )
        );

        assertTrue(S3ObjectStoragePresignTaggingTest.signedHeaders(upload.url()).contains("x-amz-tagging"));

        byte[] body = "%PDF-1.4 test".getBytes();
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

        HttpResponse<String> tagged = http.send(
                put(upload.url(), upload.headers(), body),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, tagged.statusCode(), "PUT con headers firmados: " + tagged.body());

        try (S3Client s3 = minioClient(properties)) {
            var tagging = s3.getObjectTagging(GetObjectTaggingRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build());
            assertTrue(tagging.tagSet().stream().anyMatch(this::isStagingTag),
                    "El objeto debe quedar tagueado inkcore-staging=true, tags=" + tagging.tagSet());
            s3.deleteObject(builder -> builder.bucket(properties.getBucket()).key(objectKey));
        }

        String missingKey = "tmp/company/it/ink-estimates/u1/" + UUID.randomUUID() + "/original.pdf";
        PresignedUpload upload2 = adapter.createPresignedPutUrl(
                missingKey,
                "application/pdf",
                Duration.ofSeconds(300),
                Map.of("company-id", "it"),
                Map.of(
                        InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_KEY,
                        InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_VALUE
                )
        );
        HttpResponse<String> rejected = http.send(
                put(upload2.url(), withoutTagFrom(upload2.headers()), body),
                HttpResponse.BodyHandlers.ofString()
        );
        assertTrue(rejected.statusCode() >= 400,
                "PUT sin x-amz-tagging debe fallar la firma, status=" + rejected.statusCode()
                        + " body=" + rejected.body());
    }

    private boolean isStagingTag(Tag tag) {
        return InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_KEY.equals(tag.key())
                && InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_VALUE.equals(tag.value());
    }

    private static Map<String, String> withoutTagFrom(Map<String, String> headers) {
        Map<String, String> copy = new java.util.LinkedHashMap<>();
        headers.forEach((name, value) -> {
            if (!"x-amz-tagging".equalsIgnoreCase(name)) {
                copy.put(name, value);
            }
        });
        return copy;
    }

    private static HttpRequest put(String url, Map<String, String> headers, byte[] body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        headers.forEach(builder::header);
        return builder.build();
    }

    private static boolean minioReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 9100), 400);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static ObjectStorageProperties minioProperties() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setType("s3");
        properties.setBucket("inkcore");
        properties.setRegion("us-east-1");
        properties.setAccessKey("minioadmin");
        properties.setSecretKey("minioadmin");
        properties.setPathStyleAccess(true);
        properties.setEndpoint("http://127.0.0.1:9100");
        properties.setPublicEndpoint("http://127.0.0.1:9100");
        properties.setCorsAllowedOrigins("");
        properties.setStagingExpirationDays(2);
        properties.setStagingFallbackExpirationDays(30);
        properties.setManageBucket(true);
        properties.setStartupRetryAttempts(3);
        properties.setStartupRetryBackoffMs(250);
        return properties;
    }

    private static S3Client minioClient(ObjectStorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .endpointOverride(URI.create(properties.getEndpoint()))
                .build();
    }
}
