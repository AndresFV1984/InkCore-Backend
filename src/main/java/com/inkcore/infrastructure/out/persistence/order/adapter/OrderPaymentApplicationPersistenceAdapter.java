package com.inkcore.infrastructure.out.persistence.order.adapter;

import com.inkcore.domain.order.model.OrderPaymentApplication;
import com.inkcore.domain.order.model.PaymentApplicationStatus;
import com.inkcore.domain.order.ports.out.OrderPaymentApplicationRepositoryPort;
import com.inkcore.infrastructure.out.persistence.order.mapper.OrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.order.repository.JpaOrderPaymentApplicationRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderPaymentApplicationPersistenceAdapter implements OrderPaymentApplicationRepositoryPort {

    private final JpaOrderPaymentApplicationRepository repository;
    private final EntityManager entityManager;

    public OrderPaymentApplicationPersistenceAdapter(
            JpaOrderPaymentApplicationRepository repository,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public OrderPaymentApplication save(OrderPaymentApplication application) {
        return OrderPersistenceMapper.toDomain(
                repository.saveAndFlush(OrderPersistenceMapper.toEntity(application))
        );
    }

    @Override
    public List<OrderPaymentApplication> saveAll(List<OrderPaymentApplication> applications) {
        return applications.stream().map(this::save).toList();
    }

    @Override
    public List<OrderPaymentApplication> findByPaymentId(String companyId, String paymentId) {
        return repository
                .findAllByCompanyIdAndPaymentIdOrderByAppliedAtAscCreatedAtAsc(companyId, paymentId)
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<OrderPaymentApplication> findByCxcDocumentId(String companyId, String cxcDocumentId) {
        return repository
                .findAllByCompanyIdAndCxcDocumentIdAndStatusOrderByAppliedAtAsc(
                        companyId, cxcDocumentId, PaymentApplicationStatus.ACTIVO.getDbValue())
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<OrderPaymentApplication> findActiveFavorByProductionOrderId(
            String companyId,
            String productionOrderId
    ) {
        return repository
                .findAllByCompanyIdAndProductionOrderIdAndCxcDocumentIdIsNullAndStatusOrderByAppliedAtAsc(
                        companyId, productionOrderId, PaymentApplicationStatus.ACTIVO.getDbValue())
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumActiveFavor(String companyId, String productionOrderId) {
        Object favor = entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(amount), 0)
                FROM indicolors.order_payment_applications
                WHERE company_id = :companyId
                  AND production_order_id = :productionOrderId
                  AND cxc_document_id IS NULL
                  AND status = 'activo'
                """)
                .setParameter("companyId", companyId)
                .setParameter("productionOrderId", productionOrderId)
                .getSingleResult();
        Object consumed = entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(amount), 0)
                FROM indicolors.order_payment_applications
                WHERE company_id = :companyId
                  AND production_order_id = :productionOrderId
                  AND origin_application_id IS NOT NULL
                  AND status = 'activo'
                """)
                .setParameter("companyId", companyId)
                .setParameter("productionOrderId", productionOrderId)
                .getSingleResult();
        return new BigDecimal(favor.toString()).subtract(new BigDecimal(consumed.toString()));
    }

    @Override
    @Transactional
    public void reverseAllForPayment(String companyId, String paymentId) {
        repository.reverseAllActiveForPayment(companyId, paymentId);
        repository.flush();
    }
}
