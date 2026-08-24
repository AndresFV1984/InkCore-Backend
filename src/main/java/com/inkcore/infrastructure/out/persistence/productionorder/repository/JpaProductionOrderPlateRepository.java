package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPlateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaProductionOrderPlateRepository
        extends JpaRepository<ProductionOrderPlateEntity, String> {

    List<ProductionOrderPlateEntity> findAllByProductionOrderIdOrderByCreatedAtAsc(String productionOrderId);

    void deleteAllByProductionOrderId(String productionOrderId);
}
