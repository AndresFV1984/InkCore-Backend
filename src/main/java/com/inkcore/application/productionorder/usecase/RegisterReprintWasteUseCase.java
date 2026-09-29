package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.model.WasteRecord;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class RegisterReprintWasteUseCase {

    private final ProductionOrderSupport support;
    private final ProductionOrderCostingCoordinator costing;

    public RegisterReprintWasteUseCase(
            ProductionOrderSupport support,
            ProductionOrderCostingCoordinator costing
    ) {
        this.support = support;
        this.costing = costing;
    }

    @Transactional
    public WasteRecord execute(
            String productionOrderId,
            String phase,
            BigDecimal quantity,
            String wasteReason,
            String machineId,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        var order = support.requireOrder(productionOrderId, companyId);
        return costing.registerReprint(order, phase, quantity, wasteReason, machineId);
    }
}
