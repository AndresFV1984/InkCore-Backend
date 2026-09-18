package com.inkcore.infrastructure.out.persistence.order.adapter;

import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
import com.inkcore.infrastructure.out.persistence.order.mapper.OrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.order.repository.JpaOrderDeliveryRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class OrderDeliveryPersistenceAdapter implements OrderDeliveryRepositoryPort {

    private final JpaOrderDeliveryRepository repository;
    private final EntityManager entityManager;

    public OrderDeliveryPersistenceAdapter(
            JpaOrderDeliveryRepository repository,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public OrderDelivery save(OrderDelivery delivery) {
        var entity = repository.saveAndFlush(OrderPersistenceMapper.toEntity(delivery));
        entityManager.refresh(entity);
        return OrderPersistenceMapper.toDomain(entity);
    }

    @Override
    public Optional<OrderDelivery> findById(String companyId, String orderDeliveryId) {
        return repository.findByCompanyIdAndOrderDeliveryId(companyId, orderDeliveryId)
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public Optional<OrderDelivery> findByIdForUpdate(String companyId, String orderDeliveryId) {
        return repository.findForUpdateByCompanyIdAndOrderDeliveryId(companyId, orderDeliveryId)
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public List<OrderDelivery> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository.findAllByCompanyIdAndProductionOrderIdOrderByDeliveredAtDesc(companyId, productionOrderId)
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsReversionFor(String companyId, String reversedDeliveryId) {
        return repository.existsByCompanyIdAndReversedDeliveryId(companyId, reversedDeliveryId);
    }

    @Override
    @Transactional
    public long allocateNextDeliverySequence(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new IllegalArgumentException("companyId es obligatorio para asignar delivery_number");
        }
        Object result = entityManager.createNativeQuery("""
                INSERT INTO indicolors.order_delivery_number_sequences (company_id, last_value)
                SELECT :companyId,
                       COALESCE(
                           (SELECT MAX(CAST(substring(d.delivery_number FROM 5) AS BIGINT))
                            FROM indicolors.order_deliveries d
                            WHERE d.company_id = :companyId
                              AND d.delivery_number ~ '^ODP-[0-9]+$'),
                           0
                       ) + 1
                ON CONFLICT (company_id) DO UPDATE
                SET last_value = order_delivery_number_sequences.last_value + 1
                RETURNING last_value
                """)
                .setParameter("companyId", companyId.trim())
                .getSingleResult();
        return ((Number) result).longValue();
    }
}
