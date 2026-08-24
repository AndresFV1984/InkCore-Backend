package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPostpressLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface JpaProductionOrderPostpressLineRepository
        extends JpaRepository<ProductionOrderPostpressLineEntity, String> {

    List<ProductionOrderPostpressLineEntity> findAllByRecordIdInOrderByCreatedAtAsc(Collection<String> recordIds);

    void deleteAllByRecordIdIn(Collection<String> recordIds);
}
