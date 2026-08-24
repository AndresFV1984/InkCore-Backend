package com.inkcore.application.inkestimateasset;

import com.inkcore.domain.objectstorage.ports.out.ObjectStoragePort;
import com.inkcore.domain.productionorder.service.InkEstimateAssetKeyPolicy;
import com.inkcore.domain.productionorder.service.InkEstimationSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sanitiza inkEstimation y promueve objetos de staging (tmp/) al prefijo definitivo de la OP.
 * <p>
 * Orden: {@code copyObject} de todos los tmp → nueva clave en el mapa (Postgres) →
 * {@code deleteObject} de tmp tras el commit. Si un copy falla a mitad, se borran
 * los destinos ya copiados en este intento; staging no se toca. Si Postgres hace rollback
 * después de copiar, se borran esos destinos. Si el delete de tmp falla tras commit,
 * se registra y no se revierte la OP.
 */
@Component
public class InkEstimateAssetRelocationService {

    private static final Logger log = LoggerFactory.getLogger(InkEstimateAssetRelocationService.class);

    private final ObjectStoragePort objectStorage;

    public InkEstimateAssetRelocationService(ObjectStoragePort objectStorage) {
        this.objectStorage = objectStorage;
    }

    public Map<String, Object> finalizeForPrint(
            String companyId,
            String userId,
            String productionOrderId,
            String plateId,
            Map<String, Object> inkEstimation
    ) {
        return finalizeForPrint(companyId, userId, productionOrderId, plateId, inkEstimation, null);
    }

    public Map<String, Object> finalizeForPrint(
            String companyId,
            String userId,
            String productionOrderId,
            String plateId,
            Map<String, Object> inkEstimation,
            Map<String, Object> persistedInkEstimation
    ) {
        Map<String, Object> sanitized = InkEstimationSanitizer.sanitize(inkEstimation);
        if (sanitized == null) {
            return null;
        }
        Object entriesObj = sanitized.get("entries");
        if (!(entriesObj instanceof List<?> entries) || entries.isEmpty()) {
            return sanitized;
        }
        Map<String, String> persistedKeys = indexPersistedKeys(persistedInkEstimation);
        List<String> copiedDestinations = new ArrayList<>();
        List<String> stagingToDelete = new ArrayList<>();
        try {
            List<Object> updatedEntries = new ArrayList<>();
            for (Object entryObj : entries) {
                if (entryObj instanceof Map<?, ?> rawEntry) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> entry = new LinkedHashMap<>((Map<String, Object>) rawEntry);
                    promoteEntryKeys(
                            companyId,
                            userId,
                            productionOrderId,
                            plateId,
                            entry,
                            persistedKeys,
                            copiedDestinations,
                            stagingToDelete
                    );
                    updatedEntries.add(entry);
                }
            }
            sanitized.put("entries", updatedEntries);
            registerCopiedDestinationsRollbackOnTxFailure(copiedDestinations);
            for (String stagingKey : stagingToDelete) {
                deleteStagingBestEffortAfterCommit(stagingKey);
            }
            return sanitized;
        } catch (RuntimeException ex) {
            rollbackCopiedDestinations(copiedDestinations);
            throw ex;
        }
    }

    private void promoteEntryKeys(
            String companyId,
            String userId,
            String productionOrderId,
            String plateId,
            Map<String, Object> entry,
            Map<String, String> persistedKeys,
            List<String> copiedDestinations,
            List<String> stagingToDelete
    ) {
        promoteKey(entry, "objectKey", companyId, userId, productionOrderId, plateId, false,
                persistedKeys, copiedDestinations, stagingToDelete);
        promoteKey(entry, "previewObjectKey", companyId, userId, productionOrderId, plateId, true,
                persistedKeys, copiedDestinations, stagingToDelete);
    }

    private void promoteKey(
            Map<String, Object> entry,
            String fieldName,
            String companyId,
            String userId,
            String productionOrderId,
            String plateId,
            boolean preview,
            Map<String, String> persistedKeys,
            List<String> copiedDestinations,
            List<String> stagingToDelete
    ) {
        Object current = entry.get(fieldName);
        if (!(current instanceof String currentKey) || currentKey.isBlank()) {
            return;
        }
        if (InkEstimateAssetKeyPolicy.isStagingObjectKey(currentKey)) {
            InkEstimateAssetKeyPolicy.assertStagingKeyForUser(currentKey, companyId, userId);
        } else {
            InkEstimateAssetKeyPolicy.assertReadableByCompany(currentKey, companyId);
            if (InkEstimateAssetKeyPolicy.isProductionOrderObjectKey(currentKey)) {
                InkEstimateAssetKeyPolicy.assertProductionOrderKey(currentKey, companyId, productionOrderId);
            }
            return;
        }
        String entradaId = InkEstimateAssetKeyPolicy.extractEntradaIdFromKey(currentKey);
        String destinationKey = preview
                ? InkEstimateAssetKeyPolicy.definitivePreviewKey(
                companyId, productionOrderId, plateId, entradaId)
                : InkEstimateAssetKeyPolicy.definitiveOriginalKey(
                companyId,
                productionOrderId,
                plateId,
                entradaId,
                InkEstimateAssetKeyPolicy.extractExtensionFromKey(currentKey)
        );
        if (!currentKey.equals(destinationKey)) {
            boolean alreadyPersisted = destinationKey.equals(persistedKeys.get(persistedIndexKey(entradaId, fieldName)));
            copyStagingToDestination(
                    currentKey,
                    destinationKey,
                    alreadyPersisted,
                    copiedDestinations,
                    stagingToDelete
            );
        }
        entry.put(fieldName, destinationKey);
    }

    private void copyStagingToDestination(
            String stagingKey,
            String destinationKey,
            boolean alreadyPersisted,
            List<String> copiedDestinations,
            List<String> stagingToDelete
    ) {
        if (!alreadyPersisted) {
            copiedDestinations.add(destinationKey);
        }
        try {
            objectStorage.copyObject(stagingKey, destinationKey);
            stagingToDelete.add(stagingKey);
        } catch (RuntimeException ex) {
            if (isMissingObject(ex) && (alreadyPersisted || objectStorage.exists(destinationKey))) {
                return;
            }
            throw ex;
        }
    }

    private void registerCopiedDestinationsRollbackOnTxFailure(List<String> copiedDestinations) {
        if (copiedDestinations.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        List<String> snapshot = List.copyOf(copiedDestinations);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    rollbackCopiedDestinations(snapshot);
                }
            }
        });
    }

    /**
     * Si un copy posterior falla, borra destinos ya copiados en este intento.
     * Staging no se toca (el delete es after-commit). El cliente puede reintentar update-printing.
     */
    private void rollbackCopiedDestinations(List<String> copiedDestinations) {
        for (String destinationKey : copiedDestinations) {
            try {
                objectStorage.deleteObject(destinationKey);
            } catch (RuntimeException ex) {
                log.warn("No se pudo revertir el copy a {}; puede quedar huérfano: {}",
                        destinationKey, ex.getMessage());
            }
        }
    }

    private void deleteStagingBestEffortAfterCommit(String stagingKey) {
        Runnable deleteQuietly = () -> {
            try {
                objectStorage.deleteObject(stagingKey);
            } catch (RuntimeException ex) {
                log.warn("No se pudo borrar el objeto temporal {} tras promoverlo; el lifecycle lo limpiará: {}",
                        stagingKey, ex.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteQuietly.run();
                }
            });
            return;
        }
        deleteQuietly.run();
    }

    private static Map<String, String> indexPersistedKeys(Map<String, Object> persistedInkEstimation) {
        Map<String, String> index = new HashMap<>();
        if (persistedInkEstimation == null || persistedInkEstimation.isEmpty()) {
            return index;
        }
        Object entriesObj = persistedInkEstimation.get("entries");
        if (!(entriesObj instanceof List<?> entries)) {
            return index;
        }
        for (Object entryObj : entries) {
            if (entryObj instanceof Map<?, ?> rawEntry) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) rawEntry;
                indexPersistedField(index, entry, "objectKey");
                indexPersistedField(index, entry, "previewObjectKey");
            }
        }
        return index;
    }

    private static void indexPersistedField(Map<String, String> index, Map<String, Object> entry, String fieldName) {
        Object value = entry.get(fieldName);
        if (!(value instanceof String key) || key.isBlank()) {
            return;
        }
        if (!InkEstimateAssetKeyPolicy.isProductionOrderObjectKey(key)) {
            return;
        }
        try {
            String entradaId = InkEstimateAssetKeyPolicy.extractEntradaIdFromKey(key);
            index.put(persistedIndexKey(entradaId, fieldName), key);
        } catch (RuntimeException ignored) {
            // entrada ilegible: no se usa como atajo de idempotencia
        }
    }

    private static String persistedIndexKey(String entradaId, String fieldName) {
        return entradaId + '\0' + fieldName;
    }

    private static boolean isMissingObject(RuntimeException ex) {
        String message = ex.getMessage();
        return ex instanceof IllegalArgumentException
                && message != null
                && message.toLowerCase(Locale.ROOT).contains("no encontrado");
    }
}
