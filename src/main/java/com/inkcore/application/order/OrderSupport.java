package com.inkcore.application.order;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.ports.out.NegotiatedAdditionRepositoryPort;
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
    private final NegotiatedAdditionRepositoryPort negotiatedAdditionRepository;
    private final CustomerOrderRepositoryPort customerOrderRepository;
    private final AuthenticatedCompanyResolver companyResolver;
    private final Clock clock;

    public OrderSupport(
            ProductionOrderRepositoryPort productionOrderRepository,
            OrderDeliveryRepositoryPort deliveryRepository,
            OrderPaymentRepositoryPort paymentRepository,
            NegotiatedAdditionRepositoryPort negotiatedAdditionRepository,
            CustomerOrderRepositoryPort customerOrderRepository,
            AuthenticatedCompanyResolver companyResolver,
            Clock clock
    ) {
        this.productionOrderRepository = productionOrderRepository;
        this.deliveryRepository = deliveryRepository;
        this.paymentRepository = paymentRepository;
        this.negotiatedAdditionRepository = negotiatedAdditionRepository;
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

    /** Nº entrega: ENT-{n} (serie propia; ≠ odpNumber ODP del pedido, ≠ cxcNumber CXC). */
    public String nextDeliveryNumber(String companyId) {
        return "ENT-" + deliveryRepository.allocateNextDeliverySequence(companyId);
    }

    public String nextPaymentNumber(String companyId) {
        return "LIQ-" + paymentRepository.allocateNextPaymentSequence(companyId);
    }

    public String nextNegotiatedAdditionNumber(String companyId) {
        return "ADN-" + negotiatedAdditionRepository.allocateNextSequence(companyId);
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

    /**
     * Fecha de creación del pedido comercial (= nacimiento de la cuenta ABN en dashboard Abonos).
     * Distinto de {@code accounts_receivable.opened_at} (1ª entrega / apertura CxC).
     */
    public java.time.LocalDateTime resolveCustomerOrderCreatedAt(String companyId, String productionOrderId) {
        return customerOrderRepository.findByProductionOrderId(companyId, productionOrderId)
                .map(CustomerOrder::getCreatedAt)
                .orElse(null);
    }
}
