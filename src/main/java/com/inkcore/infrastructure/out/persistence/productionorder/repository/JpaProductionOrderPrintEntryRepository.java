package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderPrintEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface JpaProductionOrderPrintEntryRepository
        extends JpaRepository<ProductionOrderPrintEntryEntity, String> {

    List<ProductionOrderPrintEntryEntity> findAllByPrintIdInOrderByCreatedAtAsc(Collection<String> printIds);

    void deleteAllByPrintIdIn(Collection<String> printIds);
}
