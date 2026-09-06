package com.inkcore.domain.station.ports.out;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.station.model.StationEventFilter;
import com.inkcore.domain.station.model.StationOperationEvent;

import java.util.List;
import java.util.Optional;

public interface StationOperationEventRepositoryPort {

    StationOperationEvent save(StationOperationEvent event);

    Optional<StationOperationEvent> findById(String eventId);

    List<StationOperationEvent> findAllByProductionOrderId(String companyId, String productionOrderId);

    List<StationOperationEvent> findAllByProductionOrderIdAndProcessKey(
            String companyId,
            String productionOrderId,
            String processKey
    );

    PageResult<StationOperationEvent> findPage(StationEventFilter filter, PageQuery pageQuery);

    List<StationOperationEvent> findByCompanyIdAndUserIdAndDateRange(
            String companyId,
            String userId,
            java.time.LocalDateTime from,
            java.time.LocalDateTime to
    );
}
