package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationOrderCompletionService;
import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Si todas las etapas reales de la OP ya están cumplidas, la marca COMPLETED (Finalizada).
 * Idempotente: útil para sanear OPs que quedaron al 100% sin pasar de estado.
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
