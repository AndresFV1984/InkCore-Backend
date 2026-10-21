package com.inkcore.application.station;

import com.inkcore.application.station.usecase.ListStationOrderProcessesUseCase;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStatus;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuando todas las etapas de estación tienen procesada la cantidad solicitada,
 * pasa la OP a {@link ProductionOrderStatus#COMPLETED} (Finalizada).
 */
@Component
public class StationOrderCompletionService {

    private final ListStationOrderProcessesUseCase processesUseCase;
    private final ProductionOrderRepositoryPort productionOrderRepository;

    public StationOrderCompletionService(
            ListStationOrderProcessesUseCase processesUseCase,
            ProductionOrderRepositoryPort productionOrderRepository
    ) {
        this.processesUseCase = processesUseCase;
        this.productionOrderRepository = productionOrderRepository;
    }

    /**
     * @return true si la OP quedó (o ya estaba) completada
     */
    public boolean completeIfAllStagesProcessed(ProductionOrder order, String actorUserId, LocalDateTime now) {
        if (order == null || order.getProductionOrderId() == null) {
            return false;
        }
        String status = order.getStatus();
        if (ProductionOrderStatus.COMPLETED.getWireValue().equalsIgnoreCase(status)
                || ProductionOrderStatus.isAnuladaAlias(status)) {
            return ProductionOrderStatus.COMPLETED.getWireValue().equalsIgnoreCase(status);
        }

        List<ListStationOrderProcessesUseCase.StationProcessRow> rows =
                processesUseCase.executeInternal(order.getProductionOrderId(), order.getCompanyId());
        if (rows.isEmpty() || !allStagesFullyProcessed(rows)) {
            return false;
        }

        order.setStatus(ProductionOrderStatus.COMPLETED.getWireValue());
        order.setUpdatedAt(now);
        if (actorUserId != null && !actorUserId.isBlank()) {
            order.setUpdatedBy(actorUserId);
        }
        productionOrderRepository.saveRoot(order);
        return true;
    }

    static boolean allStagesFullyProcessed(List<ListStationOrderProcessesUseCase.StationProcessRow> rows) {
        boolean anyPositive = false;
        for (ListStationOrderProcessesUseCase.StationProcessRow row : rows) {
            int total = row.totalUnits();
            if (total <= 0) {
                continue;
            }
            anyPositive = true;
            if (isStageFullyProcessed(row)) {
                continue;
            }
            return false;
        }
        return anyPositive;
    }

    /** Etapa cumplida: terminada, cupo entregado o cupo procesado. */
    static boolean isStageFullyProcessed(ListStationOrderProcessesUseCase.StationProcessRow row) {
        int total = row.totalUnits();
        if (total <= 0) {
            return true;
        }
        if ("terminado".equalsIgnoreCase(row.status())) {
            return true;
        }
        return Math.max(row.completedUnits(), row.deliveredUnits()) >= total;
    }
}
