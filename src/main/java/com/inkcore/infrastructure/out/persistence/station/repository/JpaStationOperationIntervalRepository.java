package com.inkcore.infrastructure.out.persistence.station.repository;

import com.inkcore.infrastructure.out.persistence.station.entity.StationOperationIntervalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaStationOperationIntervalRepository extends JpaRepository<StationOperationIntervalEntity, String> {

    Optional<StationOperationIntervalEntity> findFirstByCompanyIdAndUserIdAndProductionOrderIdAndProcessKeyAndIntervalKindAndOpenTrue(
            String companyId,
            String userId,
            String productionOrderId,
            String processKey,
            String intervalKind
    );

    List<StationOperationIntervalEntity> findAllByCompanyIdAndUserIdAndProductionOrderIdAndProcessKeyAndIntervalKindAndOpenTrue(
            String companyId,
            String userId,
            String productionOrderId,
            String processKey,
            String intervalKind
    );

    Optional<StationOperationIntervalEntity> findFirstByCompanyIdAndUserIdAndIntervalKindAndOpenTrue(
            String companyId,
            String userId,
            String intervalKind
    );

    List<StationOperationIntervalEntity> findAllByCompanyIdAndUserIdAndStartedAtBetweenOrderByStartedAtAsc(
            String companyId,
            String userId,
            LocalDateTime from,
            LocalDateTime to
    );

    List<StationOperationIntervalEntity> findAllByCompanyIdAndProductionOrderIdAndProcessKeyOrderByStartedAtAsc(
            String companyId,
            String productionOrderId,
            String processKey
    );
}
