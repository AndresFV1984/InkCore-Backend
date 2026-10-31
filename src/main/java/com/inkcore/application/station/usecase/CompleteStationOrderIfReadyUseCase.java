package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationOrderCompletionService;
import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acción de administrador: si todas las etapas reales ya están cumplidas,
 * marca la OP COMPLETED (Finalizada). Idempotente. No se dispara al operar etapas.
 */
@Service
public class CompleteStationOrderIfReadyUseCase {

    private final StationSupport support;
    private final StationOrderCompletionService orderCompletionService;

    public CompleteStationOrderIfReadyUseCase(
            StationSupport support,
            StationOrderCompletionService orderCompletionService
    ) {
        this.support = support;
        this.orderCompletionService = orderCompletionService;
    }

    @Transactional
    public Result execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        boolean completed = orderCompletionService.completeIfAllStagesProcessed(
                order,
                support.userId(authentication),
                support.now()
        );
        return new Result(order.getProductionOrderId(), order.getStatus(), completed);
    }

    public record Result(String productionOrderId, String status, boolean completed) {
    }
}
