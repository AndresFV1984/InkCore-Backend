package com.inkcore.infrastructure.out.persistence.productionorder.repository;

import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaProductionOrderRepository
        extends JpaRepository<ProductionOrderEntity, String>, JpaSpecificationExecutor<ProductionOrderEntity> {

    boolean existsByCompanyIdAndOrderNumber(String companyId, String orderNumber);

    @Query("""
            select e.productionOrderId
            from ProductionOrderEntity e
            where e.companyId = :companyId
              and e.state = true
              and e.status not in ('COMPLETED', 'ANULADA')
            order by e.priorityRank asc, e.createdAt asc
            """)
    List<String> findOpenWorkQueueIds(@Param("companyId") String companyId);

    @Query("""
            select e.productionOrderId
            from ProductionOrderEntity e
            where e.companyId = :companyId
              and e.state = true
              and e.status in (
                'IN_PROGRESS',
                'IN_PROGRESS_PREPRESS',
                'IN_PROGRESS_CUTTING',
                'IN_PROGRESS_PRINTING',
                'IN_PROGRESS_FINISHED_PRODUCTS',
                'IN_PROGRESS_FINISHING'
              )
            order by e.priorityRank asc, e.createdAt asc
            """)
    List<String> findInProgressIds(@Param("companyId") String companyId);

    @Query("""
            select coalesce(max(e.priorityRank), 0)
            from ProductionOrderEntity e
            where e.companyId = :companyId
            """)
    int findMaxPriorityRank(@Param("companyId") String companyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ProductionOrderEntity e
               set e.priorityRank = :priorityRank,
                   e.updatedAt = :updatedAt,
                   e.updatedBy = :updatedBy
             where e.productionOrderId = :productionOrderId
               and e.companyId = :companyId
            """)
    int updatePriorityRank(
            @Param("productionOrderId") String productionOrderId,
            @Param("companyId") String companyId,
            @Param("priorityRank") int priorityRank,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") String updatedBy
    );
}
