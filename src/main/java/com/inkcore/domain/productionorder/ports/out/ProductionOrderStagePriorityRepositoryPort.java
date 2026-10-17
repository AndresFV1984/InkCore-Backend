package com.inkcore.domain.productionorder.ports.out;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductionOrderStagePriorityRepositoryPort {

    List<StagePriority> findByCompany(String companyId);

    /**
     * Reemplaza la cola de una etapa. El primer id queda en el turno 1.
     * No toca {@code version} de la orden.
     */
    void replaceStageQueue(
            String companyId,
            String stageCode,
            List<String> orderedProductionOrderIds,
            String updatedBy,
            LocalDateTime updatedAt
    );

    record StagePriority(String productionOrderId, String stageCode, int priorityRank) {
    }
}
