package com.inkcore.infrastructure.out.persistence.order.repository;

import com.inkcore.infrastructure.out.persistence.order.entity.OrderPaymentApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaOrderPaymentApplicationRepository
        extends JpaRepository<OrderPaymentApplicationEntity, String> {

    List<OrderPaymentApplicationEntity> findAllByCompanyIdAndPaymentIdOrderByAppliedAtAscCreatedAtAsc(
            String companyId,
            String paymentId
    );

    List<OrderPaymentApplicationEntity> findAllByCompanyIdAndCxcDocumentIdAndStatusOrderByAppliedAtAsc(
            String companyId,
            String cxcDocumentId,
            String status
    );

    List<OrderPaymentApplicationEntity>
    findAllByCompanyIdAndProductionOrderIdAndCxcDocumentIdIsNullAndStatusOrderByAppliedAtAsc(
            String companyId,
            String productionOrderId,
            String status
    );

    @Modifying
    @Query("""
            UPDATE OrderPaymentApplicationEntity a
            SET a.status = 'reversado'
            WHERE a.companyId = :companyId
              AND a.paymentId = :paymentId
              AND a.status = 'activo'
            """)
    int reverseAllActiveForPayment(
            @Param("companyId") String companyId,
            @Param("paymentId") String paymentId
    );
}
