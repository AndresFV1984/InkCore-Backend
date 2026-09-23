package com.inkcore.application.order;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderPaymentRepositoryPort;
import com.inkcore.domain.order.ports.out.CustomerOrderRepositoryPort;
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
    private final OrderDeliveryRepositoryPort deliveryRepository;
    private final OrderPaymentRepositoryPort paymentRepository;
    private final CustomerOrderRepositoryPort customerOrderRepository;
    private final AuthenticatedCompanyResolver companyResolver;
    private final Clock clock;

    public OrderSupport(
            ProductionOrderRepositoryPort productionOrderRepository,
            OrderDeliveryRepositoryPort deliveryRepository,
            OrderPaymentRepositoryPort paymentRepository,
            CustomerOrderRepositoryPort customerOrderRepository,
            AuthenticatedCompanyResolver companyResolver,
            Clock clock
    ) {
        this.productionOrderRepository = productionOrderRepository;
        this.deliveryRepository = deliveryRepository;
        this.paymentRepository = paymentRepository;
        this.customerOrderRepository = customerOrderRepository;
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

    public String nextDeliveryNumber(String companyId) {
        return "ODP-" + deliveryRepository.allocateNextDeliverySequence(companyId);
    }

    public String nextPaymentNumber(String companyId) {
        return "ABN-" + paymentRepository.allocateNextPaymentSequence(companyId);
    }

    public String nextOdpNumber(String companyId) {
        return "ODP-" + customerOrderRepository.allocateNextOdpSequence(companyId);
    }

    public CustomerOrderRepositoryPort customerOrderRepository() {
        return customerOrderRepository;
    }

    /** Nº pedido comercial (customer_orders.odp_number); null si la OP aún no tiene pedido. */
    public String resolveOdpNumber(String companyId, String productionOrderId) {
        return customerOrderRepository.findByProductionOrderId(companyId, productionOrderId)
                .map(CustomerOrder::getOdpNumber)
                .orElse(null);
    }
}
