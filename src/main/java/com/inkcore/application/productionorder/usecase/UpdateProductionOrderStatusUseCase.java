package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.ports.out.CustomerOrderRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProductionOrderStatusUseCase {

    private final ProductionOrderSupport support;
    private final CustomerOrderRepositoryPort customerOrderRepository;

    public UpdateProductionOrderStatusUseCase(
            ProductionOrderSupport support,
            CustomerOrderRepositoryPort customerOrderRepository
    ) {
        this.support = support;
        this.customerOrderRepository = customerOrderRepository;
    }

    @Transactional
    public Result execute(
            String productionOrderId,
            UpdateProductionOrderStatusCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        support.assertVersion(order, command.version());

        String previousStatus = order.getStatus();
        ProductionOrderStatus targetStatus = null;

        if (command.status() != null && !command.status().isBlank()) {
            // Acepta ANULADA y aliases temporales (CANCELLED, etc.); persiste siempre canónico.
            targetStatus = ProductionOrderStatus.fromWire(command.status());
            order.setStatus(targetStatus.getWireValue());
        }
        if (command.state() != null) {
            order.setState(command.state());
        }
        order.setUpdatedAt(support.now());
        order.setUpdatedBy(userId);
        ProductionOrder saved = support.repository().saveRoot(order);

        CustomerOrder customerOrder;
        if (shouldCreateOrder(previousStatus, targetStatus)) {
            customerOrder = customerOrderRepository
                    .findByProductionOrderId(companyId, saved.getProductionOrderId())
                    .orElseGet(() -> customerOrderRepository.save(CustomerOrder.createNew(
                            companyId,
                            "ODP-" + customerOrderRepository.allocateNextOdpSequence(companyId),
                            saved.getProductionOrderId(),
                            saved.getClientId(),
                            userId,
                            support.now()
                    )));
        } else {
            customerOrder = customerOrderRepository
                    .findByProductionOrderId(companyId, saved.getProductionOrderId())
                    .orElse(null);
        }

        return new Result(saved, customerOrder);
    }

    private static boolean shouldCreateOrder(
            String previousStatus,
            ProductionOrderStatus targetStatus
    ) {
        if (targetStatus == null || !targetStatus.isInProgress()) {
            return false;
        }
        return !ProductionOrderStatus.isInProgressWire(previousStatus);
    }

    public record Result(ProductionOrder order, CustomerOrder customerOrder) {
    }
}
