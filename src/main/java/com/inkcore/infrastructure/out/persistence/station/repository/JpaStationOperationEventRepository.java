package com.inkcore.infrastructure.out.persistence.station.repository;

import com.inkcore.infrastructure.out.persistence.station.entity.StationOperationEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaStationOperationEventRepository
        extends JpaRepository<StationOperationEventEntity, String>, JpaSpecificationExecutor<StationOperationEventEntity> {

    List<StationOperationEventEntity> findAllByCompanyIdAndProductionOrderIdOrderByOccurredAtAsc(
            String companyId,
            String productionOrderId
    );

    List<StationOperationEventEntity> findAllByCompanyIdAndProductionOrderIdAndProcessKeyOrderByOccurredAtAsc(
            String companyId,
            String productionOrderId,
            String processKey
    );

    List<StationOperationEventEntity> findAllByCompanyIdAndUserIdAndOccurredAtBetweenOrderByOccurredAtAsc(
            String companyId,
            String userId,
            LocalDateTime from,
            LocalDateTime to
    );
}
