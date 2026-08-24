package com.inkcore.application.inkestimateasset.usecase;

import com.inkcore.application.inkestimateasset.InkEstimateAssetFileValidator;
import com.inkcore.application.productionorder.usecase.ProductionOrderSupport;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.service.InkEstimateAssetKeyPolicy;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresignProductionOrderInkEstimateAssetUseCase {

    private final ProductionOrderSupport orderSupport;
    private final InkEstimateAssetSupport assetSupport;
    private final InkEstimateAssetFileValidator fileValidator;

    public PresignProductionOrderInkEstimateAssetUseCase(
            ProductionOrderSupport orderSupport,
            InkEstimateAssetSupport assetSupport,
            InkEstimateAssetFileValidator fileValidator
    ) {
        this.orderSupport = orderSupport;
        this.assetSupport = assetSupport;
        this.fileValidator = fileValidator;
    }

    @Transactional
    public InkEstimateAssetPresignResult execute(
            String productionOrderId,
            PresignInkEstimateAssetCommand command,
            Authentication authentication
    ) {
        String companyId = orderSupport.companyId(authentication);
        String userId = orderSupport.userId(authentication);
        assetSupport.requirePlateAndEntrada(command);
        if (!assetSupport.wantsOriginal(command) && !assetSupport.wantsPreview(command)) {
            throw new IllegalArgumentException("file es obligatorio");
        }
        ProductionOrder order = orderSupport.requireOrder(productionOrderId, companyId);
        if (order.findPlate(command.plateId()).isEmpty()) {
            throw new ResourceNotFoundException("PLATE_NOT_FOUND", "Plancha no encontrada en la orden");
        }
        String originalKey = assetSupport.wantsOriginal(command)
                ? InkEstimateAssetKeyPolicy.definitiveOriginalKey(
                        companyId,
                        productionOrderId,
                        command.plateId(),
                        command.entradaId(),
                        InkEstimateAssetSupport.extensionOf(command.fileName()))
                : null;
        String previewKey = InkEstimateAssetKeyPolicy.definitivePreviewKey(
                companyId, productionOrderId, command.plateId(), command.entradaId());
        return PresignInkEstimateAssetAssembler.presign(
                assetSupport,
                fileValidator,
                command,
                companyId,
                userId,
                originalKey,
                previewKey,
                productionOrderId
        );
    }
}
