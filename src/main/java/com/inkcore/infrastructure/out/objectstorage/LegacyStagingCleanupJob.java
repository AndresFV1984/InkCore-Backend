package com.inkcore.infrastructure.out.objectstorage;

import com.inkcore.domain.objectstorage.ports.out.ObjectStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Barrido periódico de staging legado ({@code company/{id}/tmp/...}).
 * ILM no cubre ese path; un front viejo en caché puede seguir subiéndolo semanas
 * después del deploy si el job solo corriera al arranque.
 */
@Component
@ConditionalOnProperty(name = "inkcore.object-storage.type", havingValue = "s3", matchIfMissing = true)
public class LegacyStagingCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(LegacyStagingCleanupJob.class);

    private final ObjectStoragePort objectStorage;

    public LegacyStagingCleanupJob(ObjectStoragePort objectStorage) {
        this.objectStorage = objectStorage;
    }

    @Scheduled(
            initialDelayString = "${inkcore.object-storage.legacy-staging-cleanup-initial-delay-ms:15000}",
            fixedDelayString = "${inkcore.object-storage.legacy-staging-cleanup-interval-ms:86400000}"
    )
    public void sweepLegacyStaging() {
        try {
            objectStorage.cleanupLegacyStagingOrphans();
        } catch (RuntimeException ex) {
            log.warn("Barrido periódico de staging legado falló: {}", ex.getMessage());
        }
    }
}
