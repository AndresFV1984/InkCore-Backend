package com.inkcore.application.inkestimateasset.usecase;

import com.inkcore.application.inkestimateasset.InkEstimateAssetFileValidator;
import com.inkcore.domain.objectstorage.model.PresignedUpload;

final class PresignInkEstimateAssetAssembler {

    private PresignInkEstimateAssetAssembler() {
    }

    static InkEstimateAssetPresignResult presign(
            InkEstimateAssetSupport support,
            InkEstimateAssetFileValidator fileValidator,
            PresignInkEstimateAssetCommand command,
            String companyId,
            String userId,
            String plannedOriginalKey,
            String plannedPreviewKey,
            String productionOrderId
    ) {
        support.requirePlateAndEntrada(command);
        boolean wantsOriginal = support.wantsOriginal(command);
        boolean wantsPreview = support.wantsPreview(command);
        if (!wantsOriginal && !wantsPreview) {
            throw new IllegalArgumentException("file es obligatorio");
        }

        if (wantsOriginal) {
            fileValidator.validateOriginalMetadata(command.fileName(), command.contentType(), command.sizeBytes());
        }
        if (wantsPreview) {
            fileValidator.validatePreviewMetadata(command.previewContentType(), command.previewSizeBytes());
        }

        String objectKey;
        String contentType;
        long sizeBytes;
        String fileName;
        PresignedUpload upload = null;

        if (wantsOriginal) {
            contentType = fileValidator.resolveOriginalContentType(command.fileName(), command.contentType());
            fileName = command.fileName();
            sizeBytes = command.sizeBytes();
            objectKey = plannedOriginalKey;
            upload = support.presignedPut(objectKey, contentType, companyId, userId, command);
        } else {
            String existing = command.existingObjectKey() == null ? "" : command.existingObjectKey().trim();
            if (existing.isBlank()) {
                throw new IllegalArgumentException("existingObjectKey es obligatorio cuando no hay original");
            }
            support.assertAuthorizedKey(existing, companyId, userId, productionOrderId);
            objectKey = existing;
            fileName = InkEstimateAssetSupport.fileNameFromKey(existing);
            contentType = fileValidator.resolveOriginalContentType(fileName, null);
            sizeBytes = 0L;
        }

        PresignedUpload previewUpload = null;
        String previewObjectKey = null;
        if (wantsPreview) {
            previewObjectKey = plannedPreviewKey;
            previewUpload = support.presignedPut(previewObjectKey, "image/jpeg", companyId, userId, command);
        }

        return new InkEstimateAssetPresignResult(
                objectKey,
                previewObjectKey,
                contentType,
                sizeBytes,
                fileName,
                upload,
                previewUpload
        );
    }
}
