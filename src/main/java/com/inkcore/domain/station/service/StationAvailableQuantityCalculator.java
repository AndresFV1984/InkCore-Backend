package com.inkcore.domain.station.service;

import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PostpressType;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationPhase;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcula cantidad disponible para pedidos comerciales:
 * {@code MIN(unidades procesadas de cada proceso real de la OP)}.
 * Terminados/Acabados con ítems: valor de fase = mínimo entre ítems (no suma).
 * No resta entregas de pedidos OPE.
 */
public final class StationAvailableQuantityCalculator {

    private StationAvailableQuantityCalculator() {
    }

    public static int calculate(ProductionOrder order, List<StationOperationEvent> events) {
        if (order == null) {
            return 0;
        }
        List<StationOperationEvent> safeEvents = events == null ? List.of() : events;
        List<Integer> processUnits = new ArrayList<>();

        processUnits.add(unitsForBaseProcess(safeEvents, StationPhase.PREPRENSA.getApiValue()));
        processUnits.add(unitsForBaseProcess(safeEvents, StationPhase.CORTE_PAPEL.getApiValue()));
        processUnits.add(unitsForBaseProcess(safeEvents, StationPhase.IMPRESION.getApiValue()));

        phaseMinFromCatalogItems(order, safeEvents, PostpressType.FINISHED_PRODUCT, "terminado:")
                .ifPresent(processUnits::add);
        phaseMinFromCatalogItems(order, safeEvents, PostpressType.FINISHING_PROCESS, "acabado:")
                .ifPresent(processUnits::add);

        return processUnits.stream().mapToInt(Integer::intValue).min().orElse(0);
    }

    private static int unitsForBaseProcess(List<StationOperationEvent> events, String processKey) {
        return StationValidationService.sumUnits(events, StationEventType.AVANCE_UNIDADES, processKey);
    }

    private static java.util.OptionalInt phaseMinFromCatalogItems(
            ProductionOrder order,
            List<StationOperationEvent> events,
            PostpressType type,
            String processKeyPrefix
    ) {
        List<PostpressLine> lines = order.getPostpressRecords().stream()
                .filter(record -> type == record.getType())
                .flatMap(record -> record.getLines().stream())
                .filter(line -> line.getCatalogItemId() != null && !line.getCatalogItemId().isBlank())
                .toList();
        if (lines.isEmpty()) {
            return java.util.OptionalInt.empty();
        }
        int min = Integer.MAX_VALUE;
        for (PostpressLine line : lines) {
            String catalogItemId = line.getCatalogItemId().trim();
            String processKey = processKeyPrefix + catalogItemId;
            int units = StationValidationService.sumUnits(
                    events, StationEventType.AVANCE_UNIDADES, processKey, catalogItemId);
            min = Math.min(min, units);
        }
        return java.util.OptionalInt.of(min == Integer.MAX_VALUE ? 0 : min);
    }
}
