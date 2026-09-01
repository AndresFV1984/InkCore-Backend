package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderOperatorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaProductionOrderOperatorRepository
        extends JpaRepository<ProductionOrderOperatorEntity, String> {

    List<ProductionOrderOperatorEntity> findAllByProductionOrderId(String productionOrderId);

    List<ProductionOrderOperatorEntity> findAllByProductionOrderIdIn(List<String> productionOrderIds);

    boolean existsByProductionOrderId(String productionOrderId);

    void deleteAllByProductionOrderId(String productionOrderId);
}
