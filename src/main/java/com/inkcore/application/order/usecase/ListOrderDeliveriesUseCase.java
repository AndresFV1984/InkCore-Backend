package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListOrderDeliveriesUseCase {

    private final OrderSupport support;
    private final OrderDeliveryRepositoryPort deliveryRepository;

    public ListOrderDeliveriesUseCase(OrderSupport support, OrderDeliveryRepositoryPort deliveryRepository) {
        this.support = support;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderDelivery> execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        support.requireActiveOrder(productionOrderId, companyId);
        return deliveryRepository.findByProductionOrderId(companyId, productionOrderId);
    }
}
