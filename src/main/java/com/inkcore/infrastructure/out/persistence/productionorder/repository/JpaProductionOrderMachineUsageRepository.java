package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderMachineUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaProductionOrderMachineUsageRepository extends JpaRepository<ProductionOrderMachineUsageEntity, String> {

    List<ProductionOrderMachineUsageEntity> findAllByCompanyIdAndProductionOrderIdOrderByPhaseAscCreatedAtAsc(
            String companyId,
            String productionOrderId
    );

    List<ProductionOrderMachineUsageEntity> findAllByCompanyIdAndProductionOrderIdAndPhase(
            String companyId,
            String productionOrderId,
            String phase
    );

    @Modifying
    @Query("""
            DELETE FROM ProductionOrderMachineUsageEntity u
            WHERE u.productionOrderId = :productionOrderId
              AND u.phase = :phase
            """)
    void deleteByProductionOrderIdAndPhase(
            @Param("productionOrderId") String productionOrderId,
            @Param("phase") String phase
    );

    @Modifying
    @Query("""
            DELETE FROM ProductionOrderMachineUsageEntity u
            WHERE u.productionOrderId = :productionOrderId
              AND u.phase <> :phase
            """)
    void deleteByProductionOrderIdAndPhaseNot(
            @Param("productionOrderId") String productionOrderId,
            @Param("phase") String phase
    );
}
