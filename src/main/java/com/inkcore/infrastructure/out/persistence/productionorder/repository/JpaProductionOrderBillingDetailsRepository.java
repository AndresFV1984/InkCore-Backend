package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderBillingDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProductionOrderBillingDetailsRepository
        extends JpaRepository<ProductionOrderBillingDetailsEntity, String> {
}
