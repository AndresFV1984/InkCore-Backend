package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderWasteRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaProductionOrderWasteRecordRepository extends JpaRepository<ProductionOrderWasteRecordEntity, String> {

    List<ProductionOrderWasteRecordEntity> findAllByCompanyIdAndProductionOrderIdOrderByCreatedAtAsc(
            String companyId,
            String productionOrderId
    );

    List<ProductionOrderWasteRecordEntity> findAllByCompanyIdAndProductionOrderIdAndPhaseAndWasteCategory(
            String companyId,
            String productionOrderId,
            String phase,
            String wasteCategory
    );

    @Modifying
    @Query("""
            DELETE FROM ProductionOrderWasteRecordEntity w
            WHERE w.productionOrderId = :productionOrderId
            """)
    void deleteByProductionOrderId(@Param("productionOrderId") String productionOrderId);
}
