package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.ProductionStageCode;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderStagePriorityRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/**
 * El administrador define el turno de una etapa entre las órdenes que están en proceso.
 * El turno de la orden completa no cambia.
 */
@Service
public class ReorderProductionOrderStageUseCase {

    private final ProductionOrderSupport support;
    private final ProductionOrderStagePriorityRepositoryPort stagePriorities;

    public ReorderProductionOrderStageUseCase(
            ProductionOrderSupport support,
            ProductionOrderStagePriorityRepositoryPort stagePriorities
    ) {
        this.support = support;
        this.stagePriorities = stagePriorities;
    }

    @Transactional
    public List<StageAssignment> execute(String stage, List<String> productionOrderIds, Authentication authentication) {
        ProductionStageCode stageCode = ProductionStageCode.fromCode(stage);
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        List<String> requested = productionOrderIds == null ? List.of() : productionOrderIds;
        if (requested.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new ProductionOrderBusinessRuleException("Cada orden de la etapa debe tener identificador");
        }
        if (new HashSet<>(requested).size() != requested.size()) {
            throw new ProductionOrderBusinessRuleException("La cola de la etapa no puede repetir una orden");
        }

        List<String> inProgressIds = support.repository().findInProgressIds(companyId);
        if (!new HashSet<>(inProgressIds).equals(new HashSet<>(requested))) {
            throw new ProductionOrderBusinessRuleException(
                    "La cola de la etapa debe incluir todas las órdenes en proceso"
            );
        }

        stagePriorities.replaceStageQueue(companyId, stageCode.code(), requested, userId, support.now());
        return java.util.stream.IntStream.range(0, requested.size())
                .mapToObj(index -> new StageAssignment(requested.get(index), stageCode.code(), index + 1))
                .toList();
    }

    public record StageAssignment(String productionOrderId, String stage, int priorityRank) {
    }
}
