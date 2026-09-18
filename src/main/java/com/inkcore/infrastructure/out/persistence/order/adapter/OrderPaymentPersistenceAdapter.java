package com.inkcore.infrastructure.out.persistence.order.adapter;

import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.PaymentType;
import com.inkcore.domain.order.ports.out.OrderPaymentRepositoryPort;
import com.inkcore.infrastructure.out.persistence.order.mapper.OrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.order.repository.JpaOrderPaymentRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class OrderPaymentPersistenceAdapter implements OrderPaymentRepositoryPort {

    private final JpaOrderPaymentRepository repository;
    private final EntityManager entityManager;

    public OrderPaymentPersistenceAdapter(
            JpaOrderPaymentRepository repository,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
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
    public Optional<OrderPayment> findByIdForUpdate(String companyId, String orderPaymentId) {
        return repository.findForUpdateByCompanyIdAndOrderPaymentId(companyId, orderPaymentId)
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

    @Override
    @Transactional
    public long allocateNextPaymentSequence(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new IllegalArgumentException("companyId es obligatorio para asignar payment_number");
        }
        Object result = entityManager.createNativeQuery("""
                INSERT INTO indicolors.order_payment_number_sequences (company_id, last_value)
                SELECT :companyId,
                       COALESCE(
                           (SELECT MAX(CAST(substring(p.payment_number FROM 5) AS BIGINT))
                            FROM indicolors.order_payments p
                            WHERE p.company_id = :companyId
                              AND p.payment_number ~ '^ABN-[0-9]+$'),
                           0
                       ) + 1
                ON CONFLICT (company_id) DO UPDATE
                SET last_value = order_payment_number_sequences.last_value + 1
                RETURNING last_value
                """)
                .setParameter("companyId", companyId.trim())
                .getSingleResult();
        return ((Number) result).longValue();
    }
}
