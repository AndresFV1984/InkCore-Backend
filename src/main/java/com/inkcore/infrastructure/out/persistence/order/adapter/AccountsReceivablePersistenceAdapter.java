package com.inkcore.infrastructure.out.persistence.order.adapter;

import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.order.entity.AccountsReceivableEntity;
import com.inkcore.infrastructure.out.persistence.order.mapper.OrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.order.repository.JpaAccountsReceivableRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AccountsReceivablePersistenceAdapter implements AccountsReceivableRepositoryPort {

    private final JpaAccountsReceivableRepository repository;
    private final EntityManager entityManager;

    public AccountsReceivablePersistenceAdapter(
            JpaAccountsReceivableRepository repository,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<AccountsReceivable> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository.findByCompanyIdAndProductionOrderId(companyId, productionOrderId)
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public List<AccountsReceivable> findByProductionOrderIds(String companyId, List<String> productionOrderIds) {
        if (productionOrderIds == null || productionOrderIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllByCompanyIdAndProductionOrderIdIn(companyId, productionOrderIds)
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<AccountsReceivable> findByClientId(String companyId, String clientId) {
        return repository.findAllByCompanyIdAndClientIdOrderByUpdatedAtDesc(companyId, clientId)
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<AccountsReceivable> findPage(
            String companyId,
            String status,
            String clientId,
            PageQuery pageQuery
    ) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        Page<AccountsReceivableEntity> page = repository.findAll(
                toSpecification(companyId, status, clientId),
                PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        return new PageResult<>(
                page.getContent().stream().map(OrderPersistenceMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    /**
     * Red de seguridad: pedidos comerciales sin fila AR reciben shell ABN (cxc null).
     * El camino principal es el trigger en customer_orders (V71).
     * REQUIRES_NEW: no invocar desde transacciones readOnly (p. ej. listado AR).
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void openMissingBillableAccounts(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            return;
        }
        entityManager.createNativeQuery("""
                INSERT INTO indicolors.accounts_receivable (
                    cxc_number, abonos_number, company_id, production_order_id, client_id,
                    total_units, delivered_units, pending_units,
                    total_owed, total_paid, total_remaining,
                    total_cash_paid, total_withheld, total_advance_paid,
                    opened_at, due_date, payment_term_days,
                    status, updated_at
                )
                SELECT
                    NULL,
                    indicolors.fn_next_abonos_number(co.company_id),
                    co.company_id,
                    co.production_order_id,
                    co.client_id,
                    COALESCE(po.requested_quantity, 0),
                    0,
                    GREATEST(COALESCE(po.requested_quantity, 0), 0),
                    0, 0, 0,
                    0, 0, 0,
                    NULL, NULL, COALESCE(c.credit_days, 0),
                    'pendiente', now()
                FROM indicolors.customer_orders co
                JOIN indicolors.production_orders po
                    ON po.production_order_id = co.production_order_id
                JOIN indicolors.clients c
                    ON c.client_id = co.client_id
                WHERE co.company_id = :companyId
                  AND NOT EXISTS (
                      SELECT 1
                      FROM indicolors.accounts_receivable ar
                      WHERE ar.production_order_id = co.production_order_id
                  )
                """)
                .setParameter("companyId", companyId.trim())
                .executeUpdate();
    }

    private static Specification<AccountsReceivableEntity> toSpecification(
            String companyId,
            String status,
            String clientId
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("companyId"), companyId));
            if (status != null && !status.isBlank()) {
                predicates.add(builder.equal(root.get("status"), status.trim().toLowerCase()));
            }
            if (clientId != null && !clientId.isBlank()) {
                predicates.add(builder.equal(root.get("clientId"), clientId.trim()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
