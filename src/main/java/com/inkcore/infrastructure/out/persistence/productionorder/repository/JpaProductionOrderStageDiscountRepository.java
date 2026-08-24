package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderStageDiscountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaProductionOrderStageDiscountRepository
        extends JpaRepository<ProductionOrderStageDiscountEntity, String> {

    List<ProductionOrderStageDiscountEntity> findAllByProductionOrderId(String productionOrderId);

    void deleteAllByProductionOrderId(String productionOrderId);
}
