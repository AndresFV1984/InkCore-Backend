package com.inkcore.application.inkestimateasset.usecase;

import com.inkcore.application.inkestimateasset.InkEstimateAssetFileValidator;
import com.inkcore.domain.productionorder.service.InkEstimateAssetKeyPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresignStagingInkEstimateAssetUseCase {

    private final InkEstimateAssetSupport support;
    private final InkEstimateAssetFileValidator fileValidator;

    public PresignStagingInkEstimateAssetUseCase(
            InkEstimateAssetSupport support,
            InkEstimateAssetFileValidator fileValidator
    ) {
        this.support = support;
        this.fileValidator = fileValidator;
    }

    @Transactional
    public InkEstimateAssetPresignResult execute(PresignInkEstimateAssetCommand command, Authentication authentication) {
        support.requireEntrada(command);
        if (!support.wantsOriginal(command) && !support.wantsPreview(command)) {
            throw new IllegalArgumentException("file es obligatorio");
        }
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        String originalKey = support.wantsOriginal(command)
                ? InkEstimateAssetKeyPolicy.stagingOriginalKey(
                        companyId, userId, command.entradaId(), InkEstimateAssetSupport.extensionOf(command.fileName()))
                : null;
        String previewKey = InkEstimateAssetKeyPolicy.stagingPreviewKey(companyId, userId, command.entradaId());
        return PresignInkEstimateAssetAssembler.presign(
                support,
                fileValidator,
                command,
                companyId,
                userId,
                originalKey,
                previewKey,
                null
        );
    }
}
