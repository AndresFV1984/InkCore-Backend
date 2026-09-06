package com.inkcore.infrastructure.out.persistence.order.repository;

import com.inkcore.infrastructure.out.persistence.order.entity.OrderPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaOrderPaymentRepository extends JpaRepository<OrderPaymentEntity, String> {

    Optional<OrderPaymentEntity> findByCompanyIdAndOrderPaymentId(String companyId, String orderPaymentId);

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
