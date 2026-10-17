package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderStagePriorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaProductionOrderStagePriorityRepository
        extends JpaRepository<ProductionOrderStagePriorityEntity, String> {

    List<ProductionOrderStagePriorityEntity> findByCompanyIdOrderByStageCodeAscPriorityRankAsc(String companyId);

    void deleteByCompanyIdAndStageCode(String companyId, String stageCode);
}
