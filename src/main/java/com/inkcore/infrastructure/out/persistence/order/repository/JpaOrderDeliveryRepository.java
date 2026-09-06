package com.inkcore.infrastructure.out.persistence.order.repository;

import com.inkcore.infrastructure.out.persistence.order.entity.OrderDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaOrderDeliveryRepository extends JpaRepository<OrderDeliveryEntity, String> {

    Optional<OrderDeliveryEntity> findByCompanyIdAndOrderDeliveryId(String companyId, String orderDeliveryId);

    List<OrderDeliveryEntity> findAllByCompanyIdAndProductionOrderIdOrderByDeliveredAtDesc(
            String companyId,
            String productionOrderId
    );
}
