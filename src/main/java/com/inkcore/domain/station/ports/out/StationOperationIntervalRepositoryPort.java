package com.inkcore.domain.station.ports.out;

import com.inkcore.domain.station.model.StationIntervalKind;
import com.inkcore.domain.station.model.StationOperationInterval;

import java.util.List;
import java.util.Optional;

public interface StationOperationIntervalRepositoryPort {

    StationOperationInterval save(StationOperationInterval interval);

    Optional<StationOperationInterval> findOpenInterval(
            String companyId,
            String userId,
            String productionOrderId,
            String processKey,
            StationIntervalKind kind
    );

    List<StationOperationInterval> findOpenIntervals(
            String companyId,
            String userId,
            String productionOrderId,
            String processKey,
            StationIntervalKind kind
    );

    List<StationOperationInterval> findByCompanyIdAndUserIdAndDateRange(
            String companyId,
            String userId,
            java.time.LocalDateTime from,
            java.time.LocalDateTime to
    );

    List<StationOperationInterval> findByProductionOrderIdAndProcessKey(
            String companyId,
            String productionOrderId,
            String processKey
    );

    /** Intervalos abiertos de un proceso de la OP (cualquier operario / tipo salvo filtros en servicio). */
    List<StationOperationInterval> findOpenByProductionOrderIdAndProcessKey(
            String companyId,
            String productionOrderId,
            String processKey
    );

    /** Todos los intervalos abiertos de la OP. */
    List<StationOperationInterval> findOpenByProductionOrderId(String companyId, String productionOrderId);

    List<StationOperationInterval> findOpenIntervalsByUser(String companyId, String userId, StationIntervalKind kind);

    Optional<StationOperationInterval> findOpenShiftInterval(String companyId, String userId);
}
