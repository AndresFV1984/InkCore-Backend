package com.inkcore.infrastructure.out.objectstorage;

import com.inkcore.domain.objectstorage.ports.out.ObjectStoragePort;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LegacyStagingCleanupJobTest {

    @Test
    void sweepLegacyStaging_delegatesToObjectStorage() {
        ObjectStoragePort storage = mock(ObjectStoragePort.class);
        new LegacyStagingCleanupJob(storage).sweepLegacyStaging();
        verify(storage).cleanupLegacyStagingOrphans();
    }
}
