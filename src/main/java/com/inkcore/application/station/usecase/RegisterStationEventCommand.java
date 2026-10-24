package com.inkcore.application.station.usecase;

import com.inkcore.domain.station.model.StationEventType;

import java.math.BigDecimal;
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
        StationEventType eventType,
        BigDecimal actualQuantity,
        Integer actualSetupMinutes,
        Integer actualRunMinutes,
        String machineId,
        String wasteReason,
        /** Si true, no marca la OP COMPLETED aquí (p. ej. cierre admin en lote). */
        Boolean deferOrderCompletion
) {
}
