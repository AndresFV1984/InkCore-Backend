package com.inkcore.infrastructure.out.persistence.order.adapter;

import com.inkcore.domain.order.model.CxcDocumentConcept;
import com.inkcore.domain.order.model.CxcDocumentStatus;
import com.inkcore.domain.order.model.OrderCxcDocument;
import com.inkcore.domain.order.ports.out.OrderCxcDocumentRepositoryPort;
import com.inkcore.infrastructure.out.persistence.order.entity.OrderCxcDocumentEntity;
import com.inkcore.infrastructure.out.persistence.order.mapper.OrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.order.repository.JpaOrderCxcDocumentRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class OrderCxcDocumentPersistenceAdapter implements OrderCxcDocumentRepositoryPort {

    private final JpaOrderCxcDocumentRepository repository;
    private final EntityManager entityManager;

    public OrderCxcDocumentPersistenceAdapter(
            JpaOrderCxcDocumentRepository repository,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public OrderCxcDocument save(OrderCxcDocument document) {
        return OrderPersistenceMapper.toDomain(
                repository.saveAndFlush(OrderPersistenceMapper.toEntity(document))
        );
    }

    @Override
    public OrderCxcDocument update(OrderCxcDocument document) {
        OrderCxcDocumentEntity entity = repository.findById(document.getCxcDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("CxC no encontrada"));
        OrderPersistenceMapper.copy(document, entity);
        return OrderPersistenceMapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<OrderCxcDocument> findById(String companyId, String cxcDocumentId) {
        return repository.findByCompanyIdAndCxcDocumentId(companyId, cxcDocumentId)
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public Optional<OrderCxcDocument> findByIdForUpdate(String companyId, String cxcDocumentId) {
        return repository.findForUpdateByCompanyIdAndCxcDocumentId(companyId, cxcDocumentId)
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public Optional<OrderCxcDocument> findActiveByDeliveryId(String companyId, String orderDeliveryId) {
        return repository.findByCompanyIdAndSourceDeliveryIdAndStatusNot(
                        companyId, orderDeliveryId, CxcDocumentStatus.ANULADA.getDbValue())
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public Optional<OrderCxcDocument> findActiveByAdditionId(String companyId, String orderNegotiatedAdditionId) {
        return repository.findByCompanyIdAndSourceAdditionIdAndStatusNot(
                        companyId, orderNegotiatedAdditionId, CxcDocumentStatus.ANULADA.getDbValue())
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public List<OrderCxcDocument> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository
                .findAllByCompanyIdAndProductionOrderIdOrderByIssuedAtAscCreatedAtAsc(companyId, productionOrderId)
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<OrderCxcDocument> findActiveByCompanyId(String companyId) {
        return repository
                .findAllByCompanyIdAndStatusNotOrderByIssuedAtDescCreatedAtDesc(
                        companyId, CxcDocumentStatus.ANULADA.getDbValue())
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<OrderCxcDocument> findOpenByProductionOrderAndConcept(
            String companyId,
            String productionOrderId,
            CxcDocumentConcept concept
    ) {
        return repository
                .findAllByCompanyIdAndProductionOrderIdAndConceptAndStatusInOrderByIssuedAtAscCreatedAtAsc(
                        companyId,
                        productionOrderId,
                        concept.getDbValue(),
                        List.of(
                                CxcDocumentStatus.PENDIENTE.getDbValue(),
                                CxcDocumentStatus.PARCIAL.getDbValue()
                        )
                )
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumApplied(String companyId, String cxcDocumentId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(amount), 0)
                FROM indicolors.order_payment_applications
                WHERE company_id = :companyId
                  AND cxc_document_id = :cxcDocumentId
                  AND status = 'activo'
                """)
                .setParameter("companyId", companyId)
                .setParameter("cxcDocumentId", cxcDocumentId)
                .getSingleResult();
        return new BigDecimal(result.toString());
    }

    @Override
    public long countByProductionOrderId(String companyId, String productionOrderId) {
        return repository.countByCompanyIdAndProductionOrderId(companyId, productionOrderId);
    }

    @Override
    @Transactional
    public String allocateNextCxcNumber(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            throw new IllegalArgumentException("companyId es obligatorio para asignar cxc_number");
        }
        Object result = entityManager.createNativeQuery(
                        "SELECT indicolors.fn_next_cxc_number(:companyId)"
                )
                .setParameter("companyId", companyId.trim())
                .getSingleResult();
        return result.toString();
    }
}
