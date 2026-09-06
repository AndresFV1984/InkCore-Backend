package com.inkcore.domain.station.ports.out;

import com.inkcore.domain.station.model.StationOrderProgress;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StationOrderProgressRepositoryPort {

    StationOrderProgress save(StationOrderProgress progress);

    Optional<StationOrderProgress> findByProductionOrderId(String companyId, String productionOrderId);

    List<StationOrderProgress> findByProductionOrderIds(String companyId, Collection<String> productionOrderIds);
}
