package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPostpressRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaProductionOrderPostpressRecordRepository
        extends JpaRepository<ProductionOrderPostpressRecordEntity, String> {

    List<ProductionOrderPostpressRecordEntity> findAllByProductionOrderIdOrderByCreatedAtAsc(String productionOrderId);

    void deleteAllByProductionOrderId(String productionOrderId);
}
