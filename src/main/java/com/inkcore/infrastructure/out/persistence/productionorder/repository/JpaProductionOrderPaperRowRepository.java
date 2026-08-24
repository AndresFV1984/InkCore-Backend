package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPaperRowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaProductionOrderPaperRowRepository
        extends JpaRepository<ProductionOrderPaperRowEntity, String> {

    List<ProductionOrderPaperRowEntity> findAllByProductionOrderIdOrderByCreatedAtAsc(String productionOrderId);

    void deleteAllByProductionOrderId(String productionOrderId);
}
