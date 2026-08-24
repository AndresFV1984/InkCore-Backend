package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.exception.ProductionOrderNotDeletableException;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteProductionOrderUseCase {

    private final ProductionOrderSupport support;

    public DeleteProductionOrderUseCase(ProductionOrderSupport support) {
        this.support = support;
    }

    @Transactional
    public void execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);

        if (support.repository().hasOperators(order.getProductionOrderId())
                || support.repository().isBillingCompleted(order.getProductionOrderId())) {
            throw new ProductionOrderNotDeletableException();
        }
        support.repository().deleteById(order.getProductionOrderId());
    }
}
