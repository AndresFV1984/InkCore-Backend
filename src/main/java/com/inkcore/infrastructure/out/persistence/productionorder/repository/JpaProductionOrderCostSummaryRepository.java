package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderCostSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaProductionOrderCostSummaryRepository extends JpaRepository<ProductionOrderCostSummaryEntity, String> {

    Optional<ProductionOrderCostSummaryEntity> findByProductionOrderIdAndCompanyId(
            String productionOrderId,
            String companyId
    );
}
