package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/**
 * El administrador define el orden de trabajo de la orden completa.
 * Esa posición viaja por todas las etapas; el operador solo la consulta.
 */
@Service
public class ReorderProductionOrdersUseCase {

    private final ProductionOrderSupport support;

    public ReorderProductionOrdersUseCase(ProductionOrderSupport support) {
        this.support = support;
    }

    @Transactional
    public List<PriorityAssignment> execute(List<String> productionOrderIds, Authentication authentication) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        List<String> requested = productionOrderIds == null ? List.of() : productionOrderIds;
        if (requested.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new ProductionOrderBusinessRuleException("Cada orden de la cola debe tener identificador");
        }
        if (new HashSet<>(requested).size() != requested.size()) {
            throw new ProductionOrderBusinessRuleException("La cola no puede repetir una orden");
        }

        List<String> openIds = support.repository().findOpenWorkQueueIds(companyId);
        if (!new HashSet<>(openIds).equals(new HashSet<>(requested))) {
            throw new ProductionOrderBusinessRuleException(
                    "La cola debe incluir todas las órdenes abiertas, sin finalizadas ni anuladas"
            );
        }

        support.repository().replaceWorkQueuePriority(companyId, requested, userId, support.now());
        return java.util.stream.IntStream.range(0, requested.size())
                .mapToObj(index -> new PriorityAssignment(requested.get(index), index + 1))
                .toList();
    }

    public record PriorityAssignment(String productionOrderId, int priorityRank) {
    }
}
