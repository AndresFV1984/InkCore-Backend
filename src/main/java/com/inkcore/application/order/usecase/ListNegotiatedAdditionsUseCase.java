package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.ports.out.NegotiatedAdditionRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListNegotiatedAdditionsUseCase {

    private final OrderSupport support;
    private final NegotiatedAdditionRepositoryPort additionRepository;

    public ListNegotiatedAdditionsUseCase(
            OrderSupport support,
            NegotiatedAdditionRepositoryPort additionRepository
    ) {
        this.support = support;
        this.additionRepository = additionRepository;
    }

    @Transactional(readOnly = true)
    public List<NegotiatedAddition> execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        support.requireActiveOrder(productionOrderId, companyId);
        return additionRepository.findByProductionOrderId(companyId, productionOrderId);
    }
}
