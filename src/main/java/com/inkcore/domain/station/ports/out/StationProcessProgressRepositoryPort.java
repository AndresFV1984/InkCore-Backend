package com.inkcore.domain.station.ports.out;

import com.inkcore.domain.station.model.StationProcessProgress;

import java.util.List;
import java.util.Optional;

public interface StationProcessProgressRepositoryPort {

    StationProcessProgress save(StationProcessProgress progress);

    Optional<StationProcessProgress> findByOrderAndProcessKey(
            String productionOrderId,
            String processKey
    );

    List<StationProcessProgress> findByProductionOrderId(String companyId, String productionOrderId);

    List<StationProcessProgress> findByCompanyIdAndUserId(String companyId, String userId);
}
