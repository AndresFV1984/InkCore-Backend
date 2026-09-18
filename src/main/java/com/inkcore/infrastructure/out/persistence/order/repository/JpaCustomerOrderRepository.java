package com.inkcore.infrastructure.out.persistence.order.repository;

import com.inkcore.infrastructure.out.persistence.order.entity.CustomerOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaCustomerOrderRepository extends JpaRepository<CustomerOrderEntity, String> {

    Optional<CustomerOrderEntity> findByCompanyIdAndProductionOrderId(String companyId, String productionOrderId);

    List<CustomerOrderEntity> findAllByCompanyIdAndProductionOrderIdIn(
            String companyId,
            Collection<String> productionOrderIds
    );
}
