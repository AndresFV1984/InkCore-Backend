package com.inkcore.domain.order.ports.out;

import com.inkcore.domain.order.model.OrderPayment;

import java.util.List;
import java.util.Optional;

public interface OrderPaymentRepositoryPort {

    OrderPayment save(OrderPayment payment);

    Optional<OrderPayment> findById(String companyId, String orderPaymentId);

    Optional<OrderPayment> findByIdForUpdate(String companyId, String orderPaymentId);

    List<OrderPayment> findByProductionOrderId(String companyId, String productionOrderId);

    boolean existsReversionFor(String companyId, String reversedPaymentId);

    long allocateNextPaymentSequence(String companyId);
}
