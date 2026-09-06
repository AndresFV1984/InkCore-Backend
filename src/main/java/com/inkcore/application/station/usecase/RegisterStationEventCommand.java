package com.inkcore.application.station.usecase;

import com.inkcore.domain.station.model.StationEventType;

import java.time.LocalDateTime;

public record RegisterStationEventCommand(
        String productionOrderId,
        String workName,
        String phase,
        String processKey,
        String userId,
        Integer units,
        String productionStatus,
        String note,
        String pauseReason,
        LocalDateTime occurredAt,
        Boolean shiftEvent,
        StationEventType eventType
) {
}
