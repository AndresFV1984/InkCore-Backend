package com.inkcore.domain.order.ports.out;

import com.inkcore.domain.order.model.OrderPaymentApplication;

import java.math.BigDecimal;
import java.util.List;

public interface OrderPaymentApplicationRepositoryPort {

    OrderPaymentApplication save(OrderPaymentApplication application);

    List<OrderPaymentApplication> saveAll(List<OrderPaymentApplication> applications);

    List<OrderPaymentApplication> findByPaymentId(String companyId, String paymentId);

    List<OrderPaymentApplication> findByCxcDocumentId(String companyId, String cxcDocumentId);

    List<OrderPaymentApplication> findActiveFavorByProductionOrderId(String companyId, String productionOrderId);

    BigDecimal sumActiveFavor(String companyId, String productionOrderId);

    void reverseAllForPayment(String companyId, String paymentId);
}
