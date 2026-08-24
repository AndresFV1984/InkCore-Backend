package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProductionOrderUseCase {

    private final ProductionOrderSupport support;

    public GetProductionOrderUseCase(ProductionOrderSupport support) {
        this.support = support;
    }

    @Transactional(readOnly = true)
    public ProductionOrder execute(String productionOrderId, Authentication authentication) {
        return support.requireOrder(productionOrderId, support.companyId(authentication));
    }
}
