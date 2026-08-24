package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProductionOrderStatusUseCase {

    private final ProductionOrderSupport support;

    public UpdateProductionOrderStatusUseCase(ProductionOrderSupport support) {
        this.support = support;
    }

    @Transactional
    public ProductionOrder execute(
            String productionOrderId,
            UpdateProductionOrderStatusCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        support.assertVersion(order, command.version());

        if (command.status() != null && !command.status().isBlank()) {
            order.setStatus(command.status().trim().toUpperCase());
        }
        if (command.state() != null) {
            order.setState(command.state());
        }
        order.setUpdatedAt(support.now());
        order.setUpdatedBy(userId);
        return support.repository().saveRoot(order);
    }
}
