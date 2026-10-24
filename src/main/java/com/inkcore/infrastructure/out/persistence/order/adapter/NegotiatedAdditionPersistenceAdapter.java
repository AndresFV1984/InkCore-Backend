package com.inkcore.infrastructure.out.persistence.order.adapter;

import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.model.NegotiatedAdditionMovementType;
import com.inkcore.domain.order.ports.out.NegotiatedAdditionRepositoryPort;
import com.inkcore.infrastructure.out.persistence.order.entity.NegotiatedAdditionEntity;
import com.inkcore.infrastructure.out.persistence.order.mapper.OrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.order.repository.JpaNegotiatedAdditionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class NegotiatedAdditionPersistenceAdapter implements NegotiatedAdditionRepositoryPort {

    private final JpaNegotiatedAdditionRepository repository;
    private final EntityManager entityManager;

    public NegotiatedAdditionPersistenceAdapter(
            JpaNegotiatedAdditionRepository repository,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public NegotiatedAddition save(NegotiatedAddition addition) {
        return OrderPersistenceMapper.toDomain(
                repository.saveAndFlush(OrderPersistenceMapper.toEntity(addition))
        );
    }

    @Override
    public NegotiatedAddition update(NegotiatedAddition addition) {
        NegotiatedAdditionEntity entity = repository.findById(addition.getOrderNegotiatedAdditionId())
                .orElseThrow(() -> new IllegalArgumentException("Adicional negociado no encontrado"));
        OrderPersistenceMapper.copy(addition, entity);
        return OrderPersistenceMapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<NegotiatedAddition> findByIdForUpdate(String companyId, String orderNegotiatedAdditionId) {
        return repository.findForUpdateByCompanyIdAndOrderNegotiatedAdditionId(companyId, orderNegotiatedAdditionId)
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public List<NegotiatedAddition> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository
                .findAllByCompanyIdAndProductionOrderIdOrderByApprovedAtDescCreatedAtDesc(companyId, productionOrderId)
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsReversionFor(String companyId, String reversedAdditionId) {
        return repository.existsByCompanyIdAndOrderNegotiatedAdditionIdAndVoidedAtIsNotNull(
                        companyId,
                        reversedAdditionId
                )
                || repository.existsByCompanyIdAndReversedAdditionIdAndMovementType(
                        companyId,
                        reversedAdditionId,
                        NegotiatedAdditionMovementType.ANULACION.getDbValue()
                );
    }

    @Override
    public BigDecimal sumNetAmount(String companyId, String productionOrderId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(amount), 0)
                FROM indicolors.order_negotiated_additions
                WHERE company_id = :companyId
                  AND production_order_id = :productionOrderId
                  AND voided_at IS NULL
                  AND movement_type = 'registro'
                """)
                .setParameter("companyId", companyId)
                .setParameter("productionOrderId", productionOrderId)
                .getSingleResult();
        return new BigDecimal(result.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, BigDecimal> sumNetAmountByOrderIds(String companyId, Collection<String> productionOrderIds) {
        if (companyId == null || companyId.isBlank() || productionOrderIds == null || productionOrderIds.isEmpty()) {
            return Map.of();
        }
        List<String> ids = productionOrderIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT production_order_id, COALESCE(SUM(amount), 0)
                FROM indicolors.order_negotiated_additions
                WHERE company_id = :companyId
                  AND production_order_id IN (:productionOrderIds)
                  AND voided_at IS NULL
                  AND movement_type = 'registro'
                GROUP BY production_order_id
                """)
                .setParameter("companyId", companyId)
                .setParameter("productionOrderIds", ids)
                .getResultList();
        Map<String, BigDecimal> result = new java.util.LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(
                    String.valueOf(row[0]),
                    new BigDecimal(row[1].toString()).setScale(2, RoundingMode.HALF_UP)
            );
        }
        return result;
    }

    @Override
    public long allocateNextSequence(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new IllegalArgumentException("companyId es obligatorio para asignar addition_number");
        }
        Object result = entityManager.createNativeQuery("""
                INSERT INTO indicolors.order_negotiated_addition_number_sequences (company_id, last_value)
                SELECT :companyId,
                       COALESCE(
                           (SELECT MAX(CAST(substring(a.addition_number FROM 5) AS BIGINT))
                            FROM indicolors.order_negotiated_additions a
                            WHERE a.company_id = :companyId
                              AND a.addition_number ~ '^ADN-[0-9]+$'),
                           0
                       ) + 1
                ON CONFLICT (company_id) DO UPDATE
                SET last_value = order_negotiated_addition_number_sequences.last_value + 1
                RETURNING last_value
                """)
                .setParameter("companyId", companyId.trim())
                .getSingleResult();
        return ((Number) result).longValue();
    }
}
