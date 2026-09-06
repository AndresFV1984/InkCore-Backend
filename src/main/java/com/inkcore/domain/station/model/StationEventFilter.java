package com.inkcore.domain.station.model;

import java.time.LocalDateTime;

public record StationEventFilter(
        String companyId,
        String productionOrderId,
        String userId,
        String phase,
        String processKey,
        String catalogItemId,
        StationEventType eventType,
        LocalDateTime from,
        LocalDateTime to
) {
}
