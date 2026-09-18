package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.application.station.StationOrderProgressService;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrderAvailabilityUseCase {

    private final OrderSupport support;
    private final StationOrderProgressService orderProgressService;
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;

    public GetOrderAvailabilityUseCase(
            OrderSupport support,
            StationOrderProgressService orderProgressService,
            AccountsReceivableRepositoryPort accountsReceivableRepository
    ) {
        this.support = support;
        this.orderProgressService = orderProgressService;
        this.accountsReceivableRepository = accountsReceivableRepository;
    }

    @Transactional(readOnly = true)
    public Availability execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        support.requireActiveOrder(productionOrderId, companyId);

        int processed = orderProgressService.getCantidadDisponible(companyId, productionOrderId);
        int delivered = accountsReceivableRepository.findByProductionOrderId(companyId, productionOrderId)
                .map(s -> s.getDeliveredUnits())
                .orElse(0);
        int available = Math.max(0, processed - delivered);
        return new Availability(processed, delivered, available);
    }

    public record Availability(int processed, int delivered, int available) {
    }
}
