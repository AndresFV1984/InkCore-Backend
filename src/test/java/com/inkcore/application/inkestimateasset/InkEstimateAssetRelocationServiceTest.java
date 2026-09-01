package com.inkcore.application.inkestimateasset;

import com.inkcore.domain.objectstorage.exception.ObjectStorageAccessDeniedException;
import com.inkcore.domain.objectstorage.exception.ObjectStorageUnavailableException;
import com.inkcore.domain.objectstorage.ports.out.ObjectStoragePort;
import com.inkcore.domain.productionorder.service.InkEstimateAssetKeyPolicy;
import com.inkcore.infrastructure.out.objectstorage.InMemoryObjectStorageAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InkEstimateAssetRelocationServiceTest {

    private ObjectStoragePort objectStorage;
    private InkEstimateAssetRelocationService service;

    @BeforeEach
    void setUp() {
        objectStorage = new InMemoryObjectStorageAdapter();
        service = new InkEstimateAssetRelocationService(objectStorage);
    }

    @Test
    void finalizeForPrint_promotesStagingKeysAndStripsBase64() {
        String stagingKey = "tmp/company/c1/ink-estimates/u1/entry-1/original.pdf";
        String stagingPreview = "tmp/company/c1/ink-estimates/u1/entry-1/preview.jpg";
        objectStorage.putObject(stagingKey, new byte[]{1, 2, 3}, "application/pdf");
        objectStorage.putObject(stagingPreview, new byte[]{4, 5}, "image/jpeg");

        Map<String, Object> inkEstimation = new LinkedHashMap<>();
        inkEstimation.put("entries", List.of(Map.of(
                "objectKey", stagingKey,
                "previewObjectKey", stagingPreview,
                "previewImageDataUrl", "data:image/jpeg;base64,ignored",
                "totalGramsOrder", 10
        )));

        Map<String, Object> result = service.finalizeForPrint("c1", "u1", "po-1", "plate-1", inkEstimation);

        @SuppressWarnings("unchecked")
        Map<String, Object> entry = ((List<Map<String, Object>>) result.get("entries")).get(0);
        String definitiveKey = (String) entry.get("objectKey");
        String definitivePreview = (String) entry.get("previewObjectKey");

        assertTrue(definitiveKey.contains("/production-orders/po-1/prints/plate-1/"));
        assertTrue(definitivePreview.contains("/production-orders/po-1/prints/plate-1/"));
        assertEquals(10, entry.get("totalGramsOrder"));
        assertTrue(objectStorage.exists(definitiveKey));
        assertTrue(objectStorage.exists(definitivePreview));
        assertFalse(objectStorage.exists(stagingKey));
        assertFalse(objectStorage.exists(stagingPreview));
    }

    @Test
    void finalizeForPrint_rejectsForeignCompanyStagingKey() {
        String foreign = "tmp/company/other/ink-estimates/u1/entry-1/original.pdf";
        Map<String, Object> inkEstimation = Map.of("entries", List.of(Map.of("objectKey", foreign)));

        assertThrows(ObjectStorageAccessDeniedException.class, () ->
                service.finalizeForPrint("c1", "u1", "po-1", "plate-1", inkEstimation));
    }

    @Test
    void finalizeForPrint_deletesSupersededOriginalExtension() {
        String oldPdf = InkEstimateAssetKeyPolicy.definitiveOriginalKey(
                "c1", "po-1", "plate-1", "entry-1", "pdf");
        String stagingJpg = "tmp/company/c1/ink-estimates/u1/entry-1/original.jpg";
        objectStorage.putObject(oldPdf, new byte[]{1}, "application/pdf");
        objectStorage.putObject(stagingJpg, new byte[]{2, 3}, "image/jpeg");

        Map<String, Object> result = service.finalizeForPrint(
                "c1",
                "u1",
                "po-1",
                "plate-1",
                Map.of("entries", List.of(Map.of("objectKey", stagingJpg))),
                Map.of("entries", List.of(Map.of("objectKey", oldPdf)))
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> entry = ((List<Map<String, Object>>) result.get("entries")).get(0);
        String newKey = (String) entry.get("objectKey");
        assertTrue(newKey.endsWith("/original.jpg"));
        assertTrue(objectStorage.exists(newKey));
        assertFalse(objectStorage.exists(oldPdf));
        assertFalse(objectStorage.exists(stagingJpg));
    }

    @Test
    void finalizeForPrint_mergesPersistedKeysWhenAutosaveOmitsObjectKey() {
        String persistedKey = InkEstimateAssetKeyPolicy.definitiveOriginalKey(
                "c1", "po-1", "plate-1", "entry-1", "pdf");
        objectStorage.putObject(persistedKey, new byte[]{9}, "application/pdf");

        Map<String, Object> result = service.finalizeForPrint(
                "c1",
                "u1",
                "po-1",
                "plate-1",
                Map.of("entries", List.of(Map.of("id", "entry-1", "fileName", "arte.pdf"))),
                Map.of("entries", List.of(Map.of("id", "entry-1", "objectKey", persistedKey)))
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> entry = ((List<Map<String, Object>>) result.get("entries")).get(0);
        assertEquals(persistedKey, entry.get("objectKey"));
    }

    @Test
    void finalizeForPrint_doesNotFailWhenDeleteOfTmpFails() {
        String stagingKey = "tmp/company/c1/ink-estimates/u1/entry-1/original.pdf";
        ObjectStoragePort storage = mock(ObjectStoragePort.class);
        doThrow(new RuntimeException("timeout")).when(storage).deleteObject(anyString());

        InkEstimateAssetRelocationService relocator = new InkEstimateAssetRelocationService(storage);
        Map<String, Object> result = relocator.finalizeForPrint(
                "c1",
                "u1",
                "po-1",
                "plate-1",
                Map.of("entries", List.of(Map.of("objectKey", stagingKey)))
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> entry = ((List<Map<String, Object>>) result.get("entries")).get(0);
        assertTrue(((String) entry.get("objectKey")).contains("/production-orders/po-1/"));
        var order = inOrder(storage);
        order.verify(storage).copyObject(anyString(), anyString());
        order.verify(storage).deleteObject(stagingKey);
        verify(storage).copyObject(anyString(), anyString());
    }

    @Test
    void finalizeForPrint_rollsBackCopiedDestinationsWhenLaterCopyFails() {
        String stagingOriginal = "tmp/company/c1/ink-estimates/u1/entry-1/original.pdf";
        String stagingPreview = "tmp/company/c1/ink-estimates/u1/entry-1/preview.jpg";
        String destOriginal = InkEstimateAssetKeyPolicy.definitiveOriginalKey(
                "c1", "po-1", "plate-1", "entry-1", "pdf");
        String destPreview = InkEstimateAssetKeyPolicy.definitivePreviewKey(
                "c1", "po-1", "plate-1", "entry-1");

        ObjectStoragePort storage = mock(ObjectStoragePort.class);
        doThrow(new ObjectStorageUnavailableException("timeout"))
                .when(storage).copyObject(eq(stagingPreview), eq(destPreview));

        InkEstimateAssetRelocationService relocator = new InkEstimateAssetRelocationService(storage);
        assertThrows(ObjectStorageUnavailableException.class, () -> relocator.finalizeForPrint(
                "c1",
                "u1",
                "po-1",
                "plate-1",
                Map.of("entries", List.of(Map.of(
                        "objectKey", stagingOriginal,
                        "previewObjectKey", stagingPreview
                )))
        ));

        verify(storage).copyObject(stagingOriginal, destOriginal);
        verify(storage).copyObject(stagingPreview, destPreview);
        verify(storage).deleteObject(destOriginal);
        verify(storage).deleteObject(destPreview);
        verify(storage, never()).deleteObject(stagingOriginal);
        verify(storage, never()).deleteObject(stagingPreview);
    }

    @Test
    void finalizeForPrint_rollsBackCopiedDestinationsWhenTransactionRollsBack() {
        String stagingKey = "tmp/company/c1/ink-estimates/u1/entry-1/original.pdf";
        objectStorage.putObject(stagingKey, new byte[]{1, 2, 3}, "application/pdf");
        String dest = InkEstimateAssetKeyPolicy.definitiveOriginalKey("c1", "po-1", "plate-1", "entry-1", "pdf");

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.finalizeForPrint(
                    "c1",
                    "u1",
                    "po-1",
                    "plate-1",
                    Map.of("entries", List.of(Map.of("objectKey", stagingKey)))
            );
            assertTrue(objectStorage.exists(dest));
            assertTrue(objectStorage.exists(stagingKey));

            for (TransactionSynchronization sync : List.copyOf(TransactionSynchronizationManager.getSynchronizations())) {
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            }

            assertFalse(objectStorage.exists(dest));
            assertTrue(objectStorage.exists(stagingKey));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void finalizeForPrint_skipsCopyWhenPersistedKeyIsDefinitiveAndStagingIsGone() {
        String stagingKey = "tmp/company/c1/ink-estimates/u1/entry-1/original.pdf";
        String dest = InkEstimateAssetKeyPolicy.definitiveOriginalKey("c1", "po-1", "plate-1", "entry-1", "pdf");
        objectStorage.putObject(dest, new byte[]{9, 9, 9}, "application/pdf");

        Map<String, Object> result = service.finalizeForPrint(
                "c1",
                "u1",
                "po-1",
                "plate-1",
                Map.of("entries", List.of(Map.of("objectKey", stagingKey))),
                Map.of("entries", List.of(Map.of("objectKey", dest)))
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> entry = ((List<Map<String, Object>>) result.get("entries")).get(0);
        assertEquals(dest, entry.get("objectKey"));
        assertTrue(objectStorage.exists(dest));
        assertFalse(objectStorage.exists(stagingKey));
    }
}
