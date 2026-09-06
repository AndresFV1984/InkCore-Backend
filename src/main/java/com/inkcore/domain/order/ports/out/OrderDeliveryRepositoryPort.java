package com.inkcore.domain.order.ports.out;

import com.inkcore.domain.order.model.OrderDelivery;

import java.util.List;
import java.util.Optional;

public interface OrderDeliveryRepositoryPort {

    OrderDelivery save(OrderDelivery delivery);

    Optional<OrderDelivery> findById(String companyId, String orderDeliveryId);

    List<OrderDelivery> findByProductionOrderId(String companyId, String productionOrderId);
}
