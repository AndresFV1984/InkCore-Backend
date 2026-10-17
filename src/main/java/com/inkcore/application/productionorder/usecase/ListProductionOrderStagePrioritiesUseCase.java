package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.ports.out.ProductionOrderStagePriorityRepositoryPort;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderStagePriorityRepositoryPort.StagePriority;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListProductionOrderStagePrioritiesUseCase {

    private final ProductionOrderSupport support;
    private final ProductionOrderStagePriorityRepositoryPort stagePriorities;

    public ListProductionOrderStagePrioritiesUseCase(
            ProductionOrderSupport support,
            ProductionOrderStagePriorityRepositoryPort stagePriorities
    ) {
        this.support = support;
        this.stagePriorities = stagePriorities;
    }

    @Transactional(readOnly = true)
    public List<StagePriority> execute(Authentication authentication) {
        return stagePriorities.findByCompany(support.companyId(authentication));
    }
}
