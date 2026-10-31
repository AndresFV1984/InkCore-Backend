package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderOutsourcedStageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaProductionOrderOutsourcedStageRepository
        extends JpaRepository<ProductionOrderOutsourcedStageEntity, String> {

    List<ProductionOrderOutsourcedStageEntity> findAllByProductionOrderId(String productionOrderId);

    List<ProductionOrderOutsourcedStageEntity> findAllByProductionOrderIdIn(List<String> productionOrderIds);

    void deleteAllByProductionOrderId(String productionOrderId);
}
