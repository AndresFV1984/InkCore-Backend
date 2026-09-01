package com.inkcore.application.inkestimateasset.usecase;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.inkestimation.exception.InkFileTooLargeException;
import com.inkcore.domain.objectstorage.model.PresignedUpload;
import com.inkcore.domain.objectstorage.model.SignedUrl;
import com.inkcore.domain.objectstorage.ports.out.ObjectStoragePort;
import com.inkcore.domain.productionorder.service.InkEstimateAssetKeyPolicy;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class InkEstimateAssetSupport {

    private final AuthenticatedCompanyResolver companyResolver;
    private final ObjectStoragePort objectStorage;
    private final Duration presignedTtl;

    public InkEstimateAssetSupport(
            AuthenticatedCompanyResolver companyResolver,
            ObjectStoragePort objectStorage,
            InkEstimateAssetStorageSettings settings
    ) {
        this.companyResolver = companyResolver;
        this.objectStorage = objectStorage;
        this.presignedTtl = settings.presignedTtl();
    }

    public String companyId(org.springframework.security.core.Authentication authentication) {
        return companyResolver.resolveCompanyId(authentication);
    }

    public String userId(org.springframework.security.core.Authentication authentication) {
        return companyResolver.resolveUserId(authentication);
    }

    public Duration presignedTtl() {
        return presignedTtl;
    }

    public void requirePlateAndEntrada(PresignInkEstimateAssetCommand command) {
        if (command.plateId() == null || command.plateId().isBlank()) {
            throw new IllegalArgumentException("plateId es obligatorio");
        }
        requireEntrada(command);
    }

    public void requireEntrada(PresignInkEstimateAssetCommand command) {
        if (command.entradaId() == null || command.entradaId().isBlank()) {
            throw new IllegalArgumentException("entradaId es obligatorio");
        }
    }

    public boolean wantsOriginal(PresignInkEstimateAssetCommand command) {
        return command.fileName() != null && !command.fileName().isBlank();
    }

    public boolean wantsPreview(PresignInkEstimateAssetCommand command) {
        return command.previewSizeBytes() != null && command.previewSizeBytes() > 0;
    }

    public void assertAuthorizedKey(String objectKey, String companyId, String userId, String productionOrderId) {
        if (productionOrderId != null && !productionOrderId.isBlank()) {
            InkEstimateAssetKeyPolicy.assertProductionOrderKey(objectKey, companyId, productionOrderId);
            return;
        }
        InkEstimateAssetKeyPolicy.assertEstimateSourceKey(objectKey, companyId, userId);
    }

    public SignedUrl presignedGetUrl(String objectKey, String companyId, String userId, String productionOrderId) {
        assertAuthorizedKey(objectKey, companyId, userId, productionOrderId);
        if (!objectStorage.exists(objectKey)) {
            throw new ResourceNotFoundException("INK_ESTIMATE_ASSET_NOT_FOUND", "Archivo de estimación no encontrado");
        }
        return objectStorage.createPresignedGetUrl(objectKey, presignedTtl);
    }

    public void downloadAuthorizedObject(
            String objectKey,
            String companyId,
            String userId,
            String productionOrderId,
            Path destination,
            long maxBytes
    ) {
        assertAuthorizedKey(objectKey, companyId, userId, productionOrderId);
        try {
            objectStorage.downloadObject(objectKey, destination, maxBytes);
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("tamaño máximo")) {
                throw new InkFileTooLargeException(
                        "El archivo supera el tamaño máximo permitido (" + maxBytes + " bytes)"
                );
            }
            if (ex.getMessage() != null && ex.getMessage().toLowerCase(Locale.ROOT).contains("no encontrado")) {
                throw new ResourceNotFoundException(
                        "INK_ESTIMATE_ASSET_NOT_FOUND",
                        "Archivo de estimación no encontrado"
                );
            }
            throw ex;
        }
    }

    public PresignedUpload presignedPut(
            String objectKey,
            String contentType,
            String companyId,
            String userId,
            PresignInkEstimateAssetCommand command
    ) {
        Map<String, String> tags = InkEstimateAssetKeyPolicy.isStagingObjectKey(objectKey)
                ? Map.of(
                        InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_KEY,
                        InkEstimateAssetKeyPolicy.STAGING_LIFECYCLE_TAG_VALUE)
                : Map.of();
        return objectStorage.createPresignedPutUrl(
                objectKey,
                contentType,
                presignedTtl,
                metadata(companyId, userId, command),
                tags
        );
    }

    public static String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "bin";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public static String fileNameFromKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return "original.bin";
        }
        int slash = objectKey.lastIndexOf('/');
        if (slash < 0 || slash == objectKey.length() - 1) {
            return objectKey;
        }
        return objectKey.substring(slash + 1);
    }

    private static Map<String, String> metadata(
            String companyId,
            String userId,
            PresignInkEstimateAssetCommand command
    ) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("company-id", companyId);
        metadata.put("user-id", userId);
        metadata.put("entrada-id", command.entradaId());
        metadata.put("plate-id", command.plateId());
        return metadata;
    }

    public record InkEstimateAssetStorageSettings(Duration presignedTtl) {
    }
}
