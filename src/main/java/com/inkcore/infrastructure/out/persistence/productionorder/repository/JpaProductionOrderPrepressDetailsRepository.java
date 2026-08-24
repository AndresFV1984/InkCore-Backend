package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPrepressDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProductionOrderPrepressDetailsRepository
        extends JpaRepository<ProductionOrderPrepressDetailsEntity, String> {
}
