package com.inkcore.infrastructure.out.objectstorage;

import com.inkcore.domain.objectstorage.exception.ObjectStorageAccessDeniedException;
import com.inkcore.domain.objectstorage.exception.ObjectStorageUnavailableException;
import com.inkcore.domain.objectstorage.model.PresignedUpload;
import com.inkcore.domain.objectstorage.model.SignedUrl;
import com.inkcore.domain.objectstorage.ports.out.ObjectStoragePort;
import com.inkcore.domain.productionorder.service.InkEstimateAssetKeyPolicy;
import com.inkcore.infrastructure.config.ObjectStorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortIncompleteMultipartUpload;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.LifecycleExpiration;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.TaggingDirective;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Adaptador S3 compatible con MinIO. Bucket privado; el navegador usa URLs prefirmadas.
 */
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorageAdapter.class);

    private final ObjectStorageProperties properties;
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private volatile boolean bucketEnsured;
    private final Object legacyCleanupLock = new Object();

    public S3ObjectStorageAdapter(ObjectStorageProperties properties) {
        this(properties, buildS3Client(properties), buildPresigner(properties), false);
    }

    S3ObjectStorageAdapter(
            ObjectStorageProperties properties,
            S3Client s3Client,
            S3Presigner presigner,
            boolean bucketEnsured
    ) {
        this.properties = properties;
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucketEnsured = bucketEnsured;
    }

    private static S3Client buildS3Client(ObjectStorageProperties properties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey()
        );
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccess())
                .build();
        var s3ClientBuilder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Configuration)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        long attemptTimeoutMs = properties.getOperationTimeoutMs();
        if (attemptTimeoutMs > 0) {
            s3ClientBuilder.overrideConfiguration(ClientOverrideConfiguration.builder()
                    .apiCallAttemptTimeout(Duration.ofMillis(attemptTimeoutMs))
                    .build());
        }
        URI internalEndpoint = parseUri(properties.getEndpoint());
        if (internalEndpoint != null) {
            s3ClientBuilder.endpointOverride(internalEndpoint);
        }
        return s3ClientBuilder.build();
    }

    private static S3Presigner buildPresigner(ObjectStorageProperties properties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey()
        );
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccess())
                .build();
        var presignerBuilder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Configuration);
        URI publicEndpoint = parseUri(properties.getPublicEndpoint());
        URI internalEndpoint = parseUri(properties.getEndpoint());
        if (publicEndpoint != null) {
            presignerBuilder.endpointOverride(publicEndpoint);
        } else if (internalEndpoint != null) {
            presignerBuilder.endpointOverride(internalEndpoint);
        }
        return presignerBuilder.build();
    }

    @Override
    public void putObject(String objectKey, byte[] content, String contentType) {
        ensureBucketExistsOnce();
        executeWithRetry("put", () -> {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
        });
    }

    @Override
    public byte[] getObject(String objectKey) {
        ensureBucketExistsOnce();
        return executeWithRetry("get", () -> {
            try {
                ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(objectKey)
                        .build());
                return bytes.asByteArray();
            } catch (NoSuchKeyException ex) {
                throw new IllegalArgumentException("Objeto no encontrado: " + objectKey, ex);
            }
        });
    }

    @Override
    public long objectSize(String objectKey) {
        ensureBucketExistsOnce();
        return executeWithRetry("head", () -> {
            try {
                HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(objectKey)
                        .build());
                Long length = head.contentLength();
                return length == null ? 0L : length;
            } catch (NoSuchKeyException ex) {
                throw new IllegalArgumentException("Objeto no encontrado: " + objectKey, ex);
            }
        });
    }

    @Override
    public void downloadObject(String objectKey, Path destination, long maxBytes) {
        ensureBucketExistsOnce();
        executeWithRetry("download", () -> downloadObjectOnce(objectKey, destination, maxBytes));
    }

    private void downloadObjectOnce(String objectKey, Path destination, long maxBytes) {
        try (var in = s3Client.getObject(GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .build());
             OutputStream out = Files.newOutputStream(
                     destination,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE)) {
            Long contentLength = in.response().contentLength();
            if (contentLength != null && contentLength > maxBytes) {
                throw new IllegalArgumentException("Objeto supera el tamaño máximo permitido");
            }
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = in.read(buf)) >= 0) {
                total += n;
                if (total > maxBytes) {
                    throw new IllegalArgumentException("Objeto supera el tamaño máximo permitido");
                }
                out.write(buf, 0, n);
            }
        } catch (NoSuchKeyException ex) {
            throw new IllegalArgumentException("Objeto no encontrado: " + objectKey, ex);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo descargar el objeto " + objectKey, ex);
        }
    }

    @Override
    public void copyObject(String sourceKey, String destinationKey) {
        ensureBucketExistsOnce();
        executeWithRetry("copy", () -> {
            CopyObjectRequest request = CopyObjectRequest.builder()
                    .sourceBucket(properties.getBucket())
                    .sourceKey(sourceKey)
                    .destinationBucket(properties.getBucket())
                    .destinationKey(destinationKey)
                    .taggingDirective(TaggingDirective.REPLACE)
                    .tagging("")
                    .build();
            s3Client.copyObject(request);
        });
    }

    @Override
    public void deleteObject(String objectKey) {
        ensureBucketExistsOnce();
        executeWithRetry("delete", () -> {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(request);
        });
    }

    @Override
    public boolean exists(String objectKey) {
        ensureBucketExistsOnce();
        return executeWithRetry("exists", () -> {
            try {
                s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(objectKey)
                        .build());
                return true;
            } catch (NoSuchKeyException ex) {
                return false;
            } catch (RuntimeException ex) {
                if (ObjectStorageS3Errors.isNotFound(ex)) {
                    return false;
                }
                throw ex;
            }
        });
    }

    @Override
    public SignedUrl createPresignedGetUrl(String objectKey, Duration ttl) {
        ensureBucketExistsOnce();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(builder -> builder
                        .bucket(properties.getBucket())
                        .key(objectKey)
                        .build())
                .build();
        String url = presigner.presignGetObject(presignRequest).url().toString();
        return new SignedUrl(url, ttl);
    }

    @Override
    public PresignedUpload createPresignedPutUrl(
            String objectKey,
            String contentType,
            Duration ttl,
            Map<String, String> userMetadata,
            Map<String, String> objectTags
    ) {
        ensureBucketExistsOnce();
        PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentType);
        if (userMetadata != null && !userMetadata.isEmpty()) {
            putBuilder.metadata(userMetadata);
        }
        String tagging = encodeObjectTags(objectTags);
        if (!tagging.isEmpty()) {
            putBuilder.tagging(tagging);
        }
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(putBuilder.build())
                .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        Map<String, String> headers = new LinkedHashMap<>();
        presigned.httpRequest().headers().forEach((name, values) -> {
            if (values == null || values.isEmpty()) {
                return;
            }
            if ("host".equalsIgnoreCase(name) || name.toLowerCase(Locale.ROOT).startsWith("x-amz-checksum")) {
                return;
            }
            if ("x-amz-sdk-checksum-algorithm".equalsIgnoreCase(name)) {
                return;
            }
            headers.put(name, values.get(0));
        });
        if (contentType != null && !contentType.isBlank()
                && headers.keySet().stream().noneMatch(h -> "content-type".equalsIgnoreCase(h))) {
            headers.put("Content-Type", contentType);
        }
        return new PresignedUpload(presigned.url().toString(), "PUT", headers, ttl);
    }

    private void ensureBucketExistsOnce() {
        if (bucketEnsured) {
            return;
        }
        synchronized (this) {
            if (bucketEnsured) {
                return;
            }
            int attempts = Math.max(1, properties.getStartupRetryAttempts());
            long backoffMs = Math.max(0L, properties.getStartupRetryBackoffMs());
            RuntimeException last = null;
            for (int attempt = 1; attempt <= attempts; attempt++) {
                try {
                    if (properties.isManageBucket()) {
                        ensureBucketExists();
                        applyBucketCors();
                        applyStagingLifecycle();
                    } else {
                        requireExistingBucket();
                    }
                    bucketEnsured = true;
                    return;
                } catch (RuntimeException ex) {
                    last = ex;
                    log.warn("Object storage no listo (intento {}/{}): {}", attempt, attempts, ex.getMessage());
                    if (attempt < attempts && backoffMs > 0) {
                        sleepQuietly(backoffMs);
                    }
                }
            }
            throw last != null ? last : new IllegalStateException("Object storage no disponible");
        }
    }

    private void requireExistingBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (NoSuchBucketException ex) {
            throw new IllegalStateException(
                    "Bucket " + properties.getBucket() + " no existe. Ejecute minio-init o cree el bucket con root.",
                    ex);
        }
    }

    private static void sleepQuietly(long backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido esperando object storage", ie);
        }
    }

    private void executeWithRetry(String operation, Runnable action) {
        executeWithRetry(operation, () -> {
            action.run();
            return null;
        });
    }

    private <T> T executeWithRetry(String operation, Supplier<T> action) {
        int attempts = Math.max(1, properties.getOperationRetryAttempts());
        long backoffMs = Math.max(0L, properties.getOperationRetryBackoffMs());
        RuntimeException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException ex) {
                last = ex;
                if (!ObjectStorageS3Errors.isRetryable(ex) || attempt == attempts) {
                    throw mapIoError(operation, ex);
                }
                log.warn("Object storage {} falló (intento {}/{}): {}", operation, attempt, attempts, ex.getMessage());
                if (backoffMs > 0) {
                    sleepQuietly(backoffMs);
                }
            }
        }
        throw mapIoError(operation, last != null ? last : new IllegalStateException("Object storage no disponible"));
    }

    private static RuntimeException mapIoError(String operation, RuntimeException ex) {
        if (ex instanceof IllegalArgumentException
                || ex instanceof ObjectStorageAccessDeniedException
                || ex instanceof ObjectStorageUnavailableException) {
            return ex;
        }
        if (ObjectStorageS3Errors.isNotFound(ex)) {
            return new IllegalArgumentException("Objeto no encontrado", ex);
        }
        if (ObjectStorageS3Errors.isForbidden(ex)) {
            return new ObjectStorageAccessDeniedException("Object storage rechazó la operación");
        }
        if (ObjectStorageS3Errors.isTransient(ex)) {
            return new ObjectStorageUnavailableException(
                    "Object storage no disponible durante " + operation + ". Reintente.",
                    ex
            );
        }
        return new IllegalStateException("Object storage falló durante " + operation, ex);
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucket()).build());
        } catch (NoSuchBucketException ex) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucket()).build());
                log.info("Bucket de object storage creado: {}", properties.getBucket());
            } catch (BucketAlreadyExistsException ignored) {
                // carrera en arranque concurrente
            }
        }
    }

    private void applyBucketCors() {
        List<String> origins = parseCsv(properties.getCorsAllowedOrigins());
        if (origins.isEmpty()) {
            return;
        }
        try {
            CORSRule rule = CORSRule.builder()
                    .allowedHeaders("*")
                    .allowedMethods("GET", "PUT", "HEAD")
                    .allowedOrigins(origins)
                    .exposeHeaders("ETag")
                    .maxAgeSeconds(300)
                    .build();
            s3Client.putBucketCors(PutBucketCorsRequest.builder()
                    .bucket(properties.getBucket())
                    .corsConfiguration(CORSConfiguration.builder().corsRules(rule).build())
                    .build());
        } catch (RuntimeException ex) {
            log.warn("No se pudo aplicar CORS del bucket {}: {}", properties.getBucket(), ex.getMessage());
        }
    }

    /**
     * ILM de staging:
     * <ul>
     *   <li>Tag {@code inkcore-staging=true}: expiración corta (p. ej. 2 días).</li>
     *   <li>Prefijo {@code tmp/}: respaldo más largo (p. ej. 30 días) si el PUT llegó sin tag.
     *       S3 Filter Prefix no admite glob en el medio de la key; por eso el staging canónico
     *       empieza por {@code tmp/}.</li>
     * </ul>
     */
    private void applyStagingLifecycle() {
        int tagDays = properties.getStagingExpirationDays();
        int fallbackDays = properties.getStagingFallbackExpirationDays();
        List<LifecycleRule> rules = new ArrayList<>();
        if (tagDays > 0) {
            rules.add(LifecycleRule.builder()
                    .id("expire-staging-ink-estimates")
                    .status(ExpirationStatus.ENABLED)
                    .filter(LifecycleRuleFilter.builder()
                            .tag(Tag.builder()
                                    .key(InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_KEY)
                                    .value(InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_VALUE)
                                    .build())
                            .build())
                    .expiration(LifecycleExpiration.builder().days(tagDays).build())
                    .abortIncompleteMultipartUpload(
                            AbortIncompleteMultipartUpload.builder().daysAfterInitiation(1).build())
                    .build());
        }
        if (fallbackDays > 0) {
            rules.add(LifecycleRule.builder()
                    .id("expire-staging-prefix-fallback")
                    .status(ExpirationStatus.ENABLED)
                    .filter(LifecycleRuleFilter.builder()
                            .prefix(InkEstimateAssetKeyPolicy.STAGING_BUCKET_PREFIX)
                            .build())
                    .expiration(LifecycleExpiration.builder().days(fallbackDays).build())
                    .build());
        }
        if (rules.isEmpty()) {
            return;
        }
        try {
            s3Client.putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationRequest.builder()
                    .bucket(properties.getBucket())
                    .lifecycleConfiguration(BucketLifecycleConfiguration.builder()
                            .rules(rules)
                            .build())
                    .build());
            log.info("Lifecycle de staging aplicado en bucket {} (tag={}d, prefix tmp/={}d)",
                    properties.getBucket(), tagDays, fallbackDays);
        } catch (RuntimeException ex) {
            log.warn("No se pudo aplicar lifecycle de staging en {}: {}", properties.getBucket(), ex.getMessage());
        }
    }

    /**
     * Huérfanos del path viejo {@code company/{id}/tmp/ink-estimates/...}:
     * no tienen el prefijo {@code tmp/} ni (casi nunca) el tag ILM.
     * Lo invoca el job periódico (no solo el arranque).
     */
    @Override
    public void cleanupLegacyStagingOrphans() {
        int days = properties.getLegacyStagingCleanupDays();
        if (days <= 0) {
            return;
        }
        synchronized (legacyCleanupLock) {
            sweepLegacyStagingOrphans(days);
        }
    }

    private void sweepLegacyStagingOrphans(int days) {
        ensureBucketExistsOnce();
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        int deleted = 0;
        int scanned = 0;
        try {
            String continuation = null;
            do {
                var page = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(properties.getBucket())
                        .prefix("company/")
                        .continuationToken(continuation)
                        .build());
                for (var object : page.contents()) {
                    scanned++;
                    String key = object.key();
                    if (!InkEstimateAssetKeyPolicy.isLegacyStagingObjectKey(key)) {
                        continue;
                    }
                    Instant modified = object.lastModified();
                    if (modified == null || modified.isAfter(cutoff)) {
                        continue;
                    }
                    try {
                        s3Client.deleteObject(DeleteObjectRequest.builder()
                                .bucket(properties.getBucket())
                                .key(key)
                                .build());
                        deleted++;
                    } catch (RuntimeException ex) {
                        log.warn("No se pudo borrar huérfano legado {}: {}", key, ex.getMessage());
                    }
                }
                continuation = Boolean.TRUE.equals(page.isTruncated()) ? page.nextContinuationToken() : null;
            } while (continuation != null && !continuation.isBlank());
            if (deleted > 0) {
                log.info("Limpieza de staging legado: {} objeto(s) borrado(s) de {} listado(s) bajo company/ (más de {} día(s))",
                        deleted, scanned, days);
            }
        } catch (RuntimeException ex) {
            log.warn("No se pudo barrer staging legado en {}: {}", properties.getBucket(), ex.getMessage());
        }
    }

    private static String encodeObjectTags(Map<String, String> objectTags) {
        if (objectTags == null || objectTags.isEmpty()) {
            return "";
        }
        return objectTags.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank()
                        && entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static URI parseUri(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return URI.create(raw.trim());
    }

    private static List<String> parseCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
