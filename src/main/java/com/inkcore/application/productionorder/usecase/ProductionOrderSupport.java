package com.inkcore.application.productionorder.usecase;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.productionorder.exception.ProductionOrderVersionConflictException;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Carga, autorización por empresa y control de versión compartidos por los
 * casos de uso del wizard.
 */
@Component
public class ProductionOrderSupport {

    private final ProductionOrderRepositoryPort repository;
    private final AuthenticatedCompanyResolver companyResolver;
    private final Clock clock;

    public ProductionOrderSupport(
            ProductionOrderRepositoryPort repository,
            AuthenticatedCompanyResolver companyResolver,
            Clock clock
    ) {
        this.repository = repository;
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

    public ProductionOrder requireOrder(String productionOrderId, String companyId) {
        ProductionOrder order = repository.findById(productionOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PRODUCTION_ORDER_NOT_FOUND",
                        "Orden de producción no encontrada"
                ));
        companyResolver.requireSameCompany(order.getCompanyId(), companyId);
        return order;
    }

    public void assertVersion(ProductionOrder order, Long expectedVersion) {
        if (expectedVersion == null) {
            throw new IllegalArgumentException("La versión es obligatoria");
        }
        Long actual = order.getVersion() == null ? 0L : order.getVersion();
        if (!actual.equals(expectedVersion)) {
            throw new ProductionOrderVersionConflictException(expectedVersion, actual);
        }
    }

    /**
     * Consecutivo corto y estable por empresa ({@code OP-1}, {@code OP-2}, ...),
     * asignado de forma atómica para evitar colisiones.
     */
    public String nextOrderNumber(String companyId) {
        return "OP-" + repository.allocateNextOrderSequence(companyId);
    }

    public ProductionOrderRepositoryPort repository() {
        return repository;
    }
}
