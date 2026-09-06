package com.inkcore.infrastructure.out.persistence.order.adapter;

import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.PaymentType;
import com.inkcore.domain.order.ports.out.OrderPaymentRepositoryPort;
import com.inkcore.infrastructure.out.persistence.order.mapper.OrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.order.repository.JpaOrderPaymentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OrderPaymentPersistenceAdapter implements OrderPaymentRepositoryPort {

    private final JpaOrderPaymentRepository repository;

    public OrderPaymentPersistenceAdapter(JpaOrderPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrderPayment save(OrderPayment payment) {
        return OrderPersistenceMapper.toDomain(
                repository.saveAndFlush(OrderPersistenceMapper.toEntity(payment))
        );
    }

    @Override
    public Optional<OrderPayment> findById(String companyId, String orderPaymentId) {
        return repository.findByCompanyIdAndOrderPaymentId(companyId, orderPaymentId)
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public List<OrderPayment> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository.findAllByCompanyIdAndProductionOrderIdOrderByPaidAtDesc(companyId, productionOrderId)
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsReversionFor(String companyId, String reversedPaymentId) {
        return repository.existsByCompanyIdAndReversedPaymentIdAndPaymentType(
                companyId, reversedPaymentId, PaymentType.REVERSION.getDbValue());
    }
}
