package com.inkcore.domain.station.service;

import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressType;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationPhase;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Calcula cantidad disponible persistida en estación ({@code station_order_progress}):
 * {@code MIN(unidades procesadas de cada proceso real de la OP)}.
 * <p>
 * Reglas alineadas con {@code ListStationOrderProcessesUseCase}:
 * <ul>
 *   <li>Etapa tercerizada COMPLETED → aporta la cantidad solicitada de la OP (sin AVANCE).</li>
 *   <li>Etapa tercerizada habilitada pero no COMPLETED → aporta 0 (bloquea pedidos).</li>
 *   <li>Terminados/Acabados con ítems de catálogo: mínimo entre ítems (no suma).</li>
 *   <li>No resta entregas comerciales OPE (eso lo hace {@code GET .../available}).</li>
 * </ul>
 */
public final class StationAvailableQuantityCalculator {

    private StationAvailableQuantityCalculator() {
    }

    public static int calculate(ProductionOrder order, List<StationOperationEvent> events) {
        if (order == null) {
            return 0;
        }
        List<StationOperationEvent> safeEvents = events == null ? List.of() : events;
        int totalUnits = Math.max(0, order.getRequestedQuantity());
        List<Integer> processUnits = new ArrayList<>();

        processUnits.add(unitsForBaseProcess(order, safeEvents, StationPhase.PREPRENSA, totalUnits));
        processUnits.add(unitsForBaseProcess(order, safeEvents, StationPhase.CORTE_PAPEL, totalUnits));
        processUnits.add(unitsForBaseProcess(order, safeEvents, StationPhase.IMPRESION, totalUnits));

        unitsForPostpressPhase(
                order,
                safeEvents,
                StationPhase.TERMINADOS,
                ProductionOrderStage.FINISHED_PRODUCTS,
                PostpressType.FINISHED_PRODUCT,
                "terminado:",
                totalUnits
        ).ifPresent(processUnits::add);

        unitsForPostpressPhase(
                order,
                safeEvents,
                StationPhase.ACABADOS,
                ProductionOrderStage.FINISHING_PROCESSES,
                PostpressType.FINISHING_PROCESS,
                "acabado:",
                totalUnits
        ).ifPresent(processUnits::add);

        return processUnits.stream().mapToInt(Integer::intValue).min().orElse(0);
    }

    private static int unitsForBaseProcess(
            ProductionOrder order,
            List<StationOperationEvent> events,
            StationPhase phase,
            int totalUnits
    ) {
        ProductionOrderStage stage = phase.getOrderStage().orElse(null);
        if (isOutsourcedFulfillmentComplete(order, stage)) {
            return totalUnits;
        }
        if (isOutsourcedEnabled(order, stage)) {
            return 0;
        }
        return StationValidationService.sumUnits(events, StationEventType.AVANCE_UNIDADES, phase.getApiValue());
    }

    private static OptionalInt unitsForPostpressPhase(
            ProductionOrder order,
            List<StationOperationEvent> events,
            StationPhase phase,
            ProductionOrderStage stage,
            PostpressType type,
            String processKeyPrefix,
            int totalUnits
    ) {
        if (isOutsourcedFulfillmentComplete(order, stage)) {
            return OptionalInt.of(totalUnits);
        }
        if (isOutsourcedEnabled(order, stage)) {
            // Misma fila que estación: etapa presente pero aún no recibida del proveedor.
            return OptionalInt.of(0);
        }
        return phaseMinFromCatalogItems(order, events, type, processKeyPrefix);
    }

    private static OptionalInt phaseMinFromCatalogItems(
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
            return OptionalInt.empty();
        }
        int min = Integer.MAX_VALUE;
        for (PostpressLine line : lines) {
            String catalogItemId = line.getCatalogItemId().trim();
            String processKey = processKeyPrefix + catalogItemId;
            int units = StationValidationService.sumUnits(
                    events, StationEventType.AVANCE_UNIDADES, processKey, catalogItemId);
            min = Math.min(min, units);
        }
        return OptionalInt.of(min == Integer.MAX_VALUE ? 0 : min);
    }

    private static boolean isOutsourcedEnabled(ProductionOrder order, ProductionOrderStage stage) {
        if (order.getOutsourcedStages() == null || stage == null) {
            return false;
        }
        return order.getOutsourcedStages().stream()
                .anyMatch(item -> item != null && item.isEnabled() && item.getStage() == stage);
    }

    private static boolean isOutsourcedFulfillmentComplete(ProductionOrder order, ProductionOrderStage stage) {
        if (order.getOutsourcedStages() == null || stage == null) {
            return false;
        }
        return order.getOutsourcedStages().stream()
                .anyMatch(item -> item != null && item.getStage() == stage && item.isFulfillmentComplete());
    }
}
