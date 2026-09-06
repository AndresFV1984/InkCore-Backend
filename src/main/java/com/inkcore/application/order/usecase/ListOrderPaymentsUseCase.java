package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.ports.out.OrderPaymentRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListOrderPaymentsUseCase {

    private final OrderSupport support;
    private final OrderPaymentRepositoryPort paymentRepository;

    public ListOrderPaymentsUseCase(OrderSupport support, OrderPaymentRepositoryPort paymentRepository) {
        this.support = support;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderPayment> execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        support.requireActiveOrder(productionOrderId, companyId);
        return paymentRepository.findByProductionOrderId(companyId, productionOrderId);
    }
}
