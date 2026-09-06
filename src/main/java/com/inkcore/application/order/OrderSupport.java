package com.inkcore.application.order;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class OrderSupport {

    private final ProductionOrderRepositoryPort productionOrderRepository;
    private final AuthenticatedCompanyResolver companyResolver;
    private final Clock clock;

    public OrderSupport(
            ProductionOrderRepositoryPort productionOrderRepository,
            AuthenticatedCompanyResolver companyResolver,
            Clock clock
    ) {
        this.productionOrderRepository = productionOrderRepository;
        this.companyResolver = companyResolver;
        this.clock = clock;
    }

    public String companyId(Authentication authentication) {
        return companyResolver.resolveCompanyId(authentication);
    }

    public String userId(Authentication authentication) {
        return companyResolver.resolveUserId(authentication);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public ProductionOrder requireActiveOrder(String productionOrderId, String companyId) {
        ProductionOrder order = productionOrderRepository.findById(productionOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PRODUCTION_ORDER_NOT_FOUND",
                        "Orden de producción no encontrada"
                ));
        companyResolver.requireSameCompany(order.getCompanyId(), companyId);
        if (!order.isState()) {
            throw new ResourceNotFoundException(
                    "PRODUCTION_ORDER_NOT_FOUND",
                    "Orden de producción no encontrada"
            );
        }
        return order;
    }

    public ProductionOrderRepositoryPort productionOrderRepository() {
        return productionOrderRepository;
    }
}
