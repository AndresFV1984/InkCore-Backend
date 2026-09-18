package com.inkcore.infrastructure.out.persistence.order.repository;

import com.inkcore.infrastructure.out.persistence.order.entity.OrderPaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface JpaOrderPaymentRepository extends JpaRepository<OrderPaymentEntity, String> {

    Optional<OrderPaymentEntity> findByCompanyIdAndOrderPaymentId(String companyId, String orderPaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderPaymentEntity> findForUpdateByCompanyIdAndOrderPaymentId(
            String companyId,
            String orderPaymentId
    );

    List<OrderPaymentEntity> findAllByCompanyIdAndProductionOrderIdOrderByPaidAtDesc(
            String companyId,
            String productionOrderId
    );

    boolean existsByCompanyIdAndReversedPaymentIdAndPaymentType(
            String companyId,
            String reversedPaymentId,
            String paymentType
    );
}
