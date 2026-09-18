package com.inkcore.infrastructure.out.persistence.order.adapter;

import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.ports.out.CustomerOrderRepositoryPort;
import com.inkcore.infrastructure.out.persistence.order.mapper.OrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.order.repository.JpaCustomerOrderRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class CustomerOrderPersistenceAdapter implements CustomerOrderRepositoryPort {

    private final JpaCustomerOrderRepository repository;
    private final EntityManager entityManager;

    public CustomerOrderPersistenceAdapter(
            JpaCustomerOrderRepository repository,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public CustomerOrder save(CustomerOrder order) {
        var entity = OrderPersistenceMapper.toEntity(order);
        return OrderPersistenceMapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<CustomerOrder> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository.findByCompanyIdAndProductionOrderId(companyId, productionOrderId)
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public List<CustomerOrder> findByProductionOrderIds(String companyId, Collection<String> productionOrderIds) {
        if (productionOrderIds == null || productionOrderIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllByCompanyIdAndProductionOrderIdIn(companyId, productionOrderIds)
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public long allocateNextOdpSequence(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new IllegalArgumentException("companyId es obligatorio para asignar odp_number");
        }
        Object result = entityManager.createNativeQuery("""
                INSERT INTO indicolors.customer_orders_number_sequences (company_id, last_value)
                SELECT :companyId,
                       COALESCE(
                           (SELECT MAX(CAST(substring(o.odp_number FROM 5) AS BIGINT))
                            FROM indicolors.customer_orders o
                            WHERE o.company_id = :companyId
                              AND o.odp_number ~ '^ODP-[0-9]+$'),
                           0
                       ) + 1
                ON CONFLICT (company_id) DO UPDATE
                SET last_value = customer_orders_number_sequences.last_value + 1
                RETURNING last_value
                """)
                .setParameter("companyId", companyId.trim())
                .getSingleResult();
        return ((Number) result).longValue();
    }
}
