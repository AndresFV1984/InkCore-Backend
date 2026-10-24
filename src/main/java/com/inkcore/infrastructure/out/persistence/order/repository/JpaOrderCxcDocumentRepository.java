package com.inkcore.infrastructure.out.persistence.order.repository;

import com.inkcore.infrastructure.out.persistence.order.entity.OrderCxcDocumentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaOrderCxcDocumentRepository extends JpaRepository<OrderCxcDocumentEntity, String> {

    Optional<OrderCxcDocumentEntity> findByCompanyIdAndCxcDocumentId(String companyId, String cxcDocumentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT d FROM OrderCxcDocumentEntity d
            WHERE d.companyId = :companyId AND d.cxcDocumentId = :cxcDocumentId
            """)
    Optional<OrderCxcDocumentEntity> findForUpdateByCompanyIdAndCxcDocumentId(
            @Param("companyId") String companyId,
            @Param("cxcDocumentId") String cxcDocumentId
    );

    Optional<OrderCxcDocumentEntity> findByCompanyIdAndSourceDeliveryIdAndStatusNot(
            String companyId,
            String sourceDeliveryId,
            String status
    );

    Optional<OrderCxcDocumentEntity> findByCompanyIdAndSourceAdditionIdAndStatusNot(
            String companyId,
            String sourceAdditionId,
            String status
    );

    List<OrderCxcDocumentEntity> findAllByCompanyIdAndProductionOrderIdOrderByIssuedAtAscCreatedAtAsc(
            String companyId,
            String productionOrderId
    );

    List<OrderCxcDocumentEntity> findAllByCompanyIdAndStatusNotOrderByIssuedAtDescCreatedAtDesc(
            String companyId,
            String status
    );

    List<OrderCxcDocumentEntity> findAllByCompanyIdAndProductionOrderIdAndConceptAndStatusInOrderByIssuedAtAscCreatedAtAsc(
            String companyId,
            String productionOrderId,
            String concept,
            List<String> statuses
    );

    long countByCompanyIdAndProductionOrderId(String companyId, String productionOrderId);
}
