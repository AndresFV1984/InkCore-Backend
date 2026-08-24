package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaProductionOrderRepository
        extends JpaRepository<ProductionOrderEntity, String>, JpaSpecificationExecutor<ProductionOrderEntity> {

    boolean existsByCompanyIdAndOrderNumber(String companyId, String orderNumber);
}
