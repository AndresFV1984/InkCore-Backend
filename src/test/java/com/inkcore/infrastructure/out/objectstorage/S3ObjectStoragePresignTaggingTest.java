package com.inkcore.infrastructure.out.objectstorage;

import com.inkcore.domain.objectstorage.model.PresignedUpload;
import com.inkcore.domain.productionorder.service.InkEstimateAssetKeyPolicy;
import com.inkcore.infrastructure.config.ObjectStorageProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3ObjectStoragePresignTaggingTest {

    private static final String STAGING_KEY = "tmp/company/c1/ink-estimates/u1/entry-1/original.pdf";

    @Test
    void presignedPut_signsXAmzTaggingHeader() {
        ObjectStorageProperties properties = testProperties();
        S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(
                properties,
                mock(S3Client.class),
                buildPresigner(properties),
                true
        );

        PresignedUpload upload = adapter.createPresignedPutUrl(
                STAGING_KEY,
                "application/pdf",
                Duration.ofSeconds(300),
                Map.of("company-id", "c1", "user-id", "u1"),
                Map.of(
                        InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_KEY,
                        InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_VALUE
                )
        );

        assertTrue(
                upload.headers().keySet().stream().anyMatch(h -> "x-amz-tagging".equalsIgnoreCase(h)),
                "El mapa de headers del PUT debe incluir x-amz-tagging (firmado, no inyectado después)"
        );
        String signed = signedHeaders(upload.url());
        assertTrue(
                signed.contains("x-amz-tagging"),
                "X-Amz-SignedHeaders debe incluir x-amz-tagging; era: " + signed
        );
        String tagging = upload.headers().entrySet().stream()
                .filter(e -> "x-amz-tagging".equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();
        assertTrue(tagging.contains(InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_KEY));
        assertTrue(tagging.contains(InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_VALUE));
    }

    @Test
    void ensureBucket_appliesTagRuleAndTmpPrefixFallback() {
        ObjectStorageProperties properties = testProperties();
        properties.setManageBucket(true);
        properties.setCorsAllowedOrigins("");
        properties.setStagingExpirationDays(2);
        properties.setStagingFallbackExpirationDays(30);
        properties.setLegacyStagingCleanupDays(0);

        S3Client s3 = mock(S3Client.class);
        when(s3.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());
        when(s3.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());

        S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(
                properties,
                s3,
                buildPresigner(properties),
                false
        );
        adapter.exists("any-key");

        ArgumentCaptor<PutBucketLifecycleConfigurationRequest> captor =
                ArgumentCaptor.forClass(PutBucketLifecycleConfigurationRequest.class);
        verify(s3).putBucketLifecycleConfiguration(captor.capture());
        List<LifecycleRule> rules = captor.getValue().lifecycleConfiguration().rules();
        assertEquals(2, rules.size());
        assertTrue(rules.stream().anyMatch(rule ->
                rule.filter() != null
                        && rule.filter().tag() != null
                        && InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_KEY.equals(rule.filter().tag().key())
                        && rule.expiration() != null
                        && Integer.valueOf(2).equals(rule.expiration().days())));
        assertTrue(rules.stream().anyMatch(rule ->
                rule.filter() != null
                        && InkEstimateAssetKeyPolicy.STAGING_BUCKET_PREFIX.equals(rule.filter().prefix())
                        && rule.expiration() != null
                        && Integer.valueOf(30).equals(rule.expiration().days())));
    }

    @Test
    void whenManageBucketDisabled_doesNotMutateBucketConfig() {
        ObjectStorageProperties properties = testProperties();
        properties.setManageBucket(false);
        properties.setStartupRetryAttempts(1);
        properties.setStartupRetryBackoffMs(0);

        S3Client s3 = mock(S3Client.class);
        when(s3.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());
        when(s3.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());

        S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(
                properties,
                s3,
                buildPresigner(properties),
                false
        );
        adapter.exists("any-key");

        verify(s3).headBucket(any(HeadBucketRequest.class));
        verify(s3, never()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
        verify(s3, never()).putBucketCors(any(PutBucketCorsRequest.class));
    }

    @Test
    void requireExistingBucket_retriesThenSucceeds() {
        ObjectStorageProperties properties = testProperties();
        properties.setManageBucket(false);
        properties.setStartupRetryAttempts(3);
        properties.setStartupRetryBackoffMs(0);

        S3Client s3 = mock(S3Client.class);
        when(s3.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder()
                        .message("not yet")
                        .build())
                .thenReturn(HeadBucketResponse.builder().build());
        when(s3.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());

        S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(
                properties,
                s3,
                buildPresigner(properties),
                false
        );
        adapter.exists("any-key");

        verify(s3, times(2)).headBucket(any(HeadBucketRequest.class));
        verify(s3, never()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
    }

    private static ObjectStorageProperties testProperties() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setBucket("inkcore");
        properties.setRegion("us-east-1");
        properties.setAccessKey("test");
        properties.setSecretKey("test");
        properties.setPathStyleAccess(true);
        properties.setEndpoint("http://localhost:9100");
        properties.setPublicEndpoint("http://localhost:9100");
        properties.setOperationRetryBackoffMs(0);
        return properties;
    }

    private static S3Presigner buildPresigner(ObjectStorageProperties properties) {
        return S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccess())
                        .build())
                .endpointOverride(URI.create(properties.getPublicEndpoint()))
                .build();
    }

    static String signedHeaders(String url) {
        String query = URI.create(url).getRawQuery();
        if (query == null) {
            return "";
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String name = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            if ("X-Amz-SignedHeaders".equalsIgnoreCase(name)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8)
                        .toLowerCase(Locale.ROOT);
            }
        }
        return "";
    }
}
