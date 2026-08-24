package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPrintEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaProductionOrderPrintRepository
        extends JpaRepository<ProductionOrderPrintEntity, String> {

    List<ProductionOrderPrintEntity> findAllByProductionOrderIdOrderByCreatedAtAsc(String productionOrderId);

    void deleteAllByProductionOrderId(String productionOrderId);
}
