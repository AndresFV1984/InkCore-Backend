package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetProductionOrderUseCase {

    private final ProductionOrderSupport support;
    private final ProductionOrderCostingCoordinator costing;

    public GetProductionOrderUseCase(
            ProductionOrderSupport support,
            ProductionOrderCostingCoordinator costing
    ) {
        this.support = support;
        this.costing = costing;
    }

    /**
     * Detalle completo. También refresca {@code quoted_price} para que Pedidos/listado
     * coincidan con el Cobro recalculado (descuentos por etapa incluidos).
     */
    @Transactional
    public ProductionOrder execute(String productionOrderId, Authentication authentication) {
        ProductionOrder order = support.requireOrder(productionOrderId, support.companyId(authentication));
        costing.syncQuotedPrice(order);
        return order;
    }
}
