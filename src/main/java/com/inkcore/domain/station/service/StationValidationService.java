package com.inkcore.domain.station.service;

import com.inkcore.domain.productionorder.model.OperatorAssignment;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.productionorder.model.ProductionOrderStatus;
import com.inkcore.domain.station.exception.StationBusinessRuleException;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationPauseReason;
import com.inkcore.domain.station.model.StationPhase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class StationValidationService {

    public static final String EXECUTION_BLOCKED_MESSAGE =
            "La orden debe estar en producción en planta para registrar avances.";

    public void validateEventRegistration(
            ProductionOrder order,
            String operatorUserId,
            StationPhase phase,
            StationProcessKeyResolver.ResolvedProcessKey resolved,
            StationEventType eventType,
            Integer units,
            String pauseReason,
            String note,
            boolean shiftEvent,
            List<StationOperationEvent> existingEvents
    ) {
        if (!shiftEvent) {
            requireExecutionAllowed(order);
            requireOperatorAssigned(order, operatorUserId, phase);
            validatePhaseSequence(order, phase, existingEvents);
        }

        if (eventType == StationEventType.AVANCE_UNIDADES
                || eventType == StationEventType.ENTREGA_PARCIAL
                || eventType == StationEventType.ENTREGA_TOTAL) {
            requirePositiveUnits(units);
            int processed = sumUnits(
                    existingEvents,
                    StationEventType.AVANCE_UNIDADES,
                    resolved.processKey(),
                    resolved.catalogItemId()
            );
            int delivered = sumDeliveryUnits(
                    existingEvents,
                    resolved.processKey(),
                    resolved.catalogItemId()
            );
            if (eventType == StationEventType.AVANCE_UNIDADES) {
                int pending = Math.max(0, resolved.totalUnits() - processed);
                if (units > pending) {
                    throw new StationBusinessRuleException(
                            "Las unidades exceden el pendiente del proceso (" + pending + ")"
                    );
                }
            } else {
                int available = processed - delivered;
                if (units > available) {
                    throw new StationBusinessRuleException(
                            "Las unidades de entrega exceden las procesadas disponibles (" + available + ")"
                    );
                }
            }
        }

        if (eventType == StationEventType.PARO) {
            requirePauseReason(pauseReason, note);
            if (hasOpenPause(existingEvents, resolved.processKey(), operatorUserId, resolved.catalogItemId())) {
                throw new StationBusinessRuleException("Ya existe una pausa abierta en este proceso");
            }
        }

        if (eventType == StationEventType.REANUDACION) {
            if (!hasOpenPause(existingEvents, resolved.processKey(), operatorUserId, resolved.catalogItemId())) {
                throw new StationBusinessRuleException(
                        "No hay pausa abierta para reanudar en este processKey"
                );
            }
        }

        if (eventType == StationEventType.MARCA_HORARIO) {
            requirePauseReason(pauseReason, note);
        }
    }

    public boolean orderAllowsOperatorExecution(ProductionOrder order) {
        return order.isState() && isInProgressStatus(order.getStatus());
    }

    public void requireExecutionAllowed(ProductionOrder order) {
        if (!orderAllowsOperatorExecution(order)) {
            throw new StationBusinessRuleException(EXECUTION_BLOCKED_MESSAGE);
        }
    }

    public void requireOperatorAssigned(ProductionOrder order, String userId, StationPhase phase) {
        if (phase == StationPhase.JORNADA) {
            return;
        }
        ProductionOrderStage stage = phase.getOrderStage()
                .orElseThrow(() -> new StationBusinessRuleException("Fase sin etapa de operador asignable"));
        boolean assigned = order.getOperators().stream()
                .anyMatch(operator -> stage == operator.getStage()
                        && Objects.equals(operator.getUserId(), userId));
        if (!assigned) {
            throw new StationBusinessRuleException("El operario no está asignado a esta fase de la orden");
        }
    }

    public void validateOccurredAt(LocalDateTime occurredAt, LocalDateTime now) {
        if (occurredAt == null) {
            throw new StationBusinessRuleException("occurredAt es obligatorio");
        }
        if (occurredAt.isAfter(now.plusMinutes(5))) {
            throw new StationBusinessRuleException("occurredAt no puede estar en el futuro lejano");
        }
    }

    private void validatePhaseSequence(
            ProductionOrder order,
            StationPhase phase,
            List<StationOperationEvent> existingEvents
    ) {
        if (phase == StationPhase.PREPRENSA || phase == StationPhase.JORNADA) {
            return;
        }
        if (phase == StationPhase.CORTE_PAPEL && order.getPrepress() == null) {
            return;
        }
        if (phase == StationPhase.IMPRESION && hasAnyEventForPhase(existingEvents, StationPhase.CORTE_PAPEL)) {
            return;
        }
        if (phase == StationPhase.TERMINADOS && hasAnyEventForPhase(existingEvents, StationPhase.IMPRESION)) {
            return;
        }
        if (phase == StationPhase.ACABADOS && hasAnyEventForPhase(existingEvents, StationPhase.TERMINADOS)) {
            return;
        }
        // Soft prerequisite: only warn via first-event allowance for non-preprensa if order already in progress
        if (phase != StationPhase.CORTE_PAPEL && !isInProgressStatus(order.getStatus())) {
            throw new StationBusinessRuleException(EXECUTION_BLOCKED_MESSAGE);
        }
    }

    private static boolean hasAnyEventForPhase(List<StationOperationEvent> events, StationPhase phase) {
        return events.stream().anyMatch(event -> phase.getApiValue().equals(event.getPhase()));
    }

    private static void requirePositiveUnits(Integer units) {
        if (units == null || units <= 0) {
            throw new StationBusinessRuleException("units debe ser mayor que 0");
        }
    }

    private static void requirePauseReason(String pauseReason, String note) {
        if (pauseReason == null || pauseReason.isBlank()) {
            throw new StationBusinessRuleException("pauseReason es obligatorio");
        }
        StationPauseReason reason = StationPauseReason.fromValue(pauseReason);
        if (reason == StationPauseReason.OTRO && (note == null || note.isBlank())) {
            throw new StationBusinessRuleException("note es obligatorio cuando pauseReason es otro");
        }
    }

    /**
     * True si la OP está en planta: {@code IN_PROGRESS} o cualquier {@code IN_PROGRESS_*}.
     * También acepta el alias legacy {@code EN PROCESO}.
     */
    public static boolean isInProgressStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim();
        if ("EN PROCESO".equalsIgnoreCase(normalized)) {
            return true;
        }
        return ProductionOrderStatus.isInProgressWire(normalized);
    }

    public static int sumUnits(List<StationOperationEvent> events, StationEventType type, String processKey) {
        return sumUnits(events, type, processKey, null);
    }

    public static int sumUnits(
            List<StationOperationEvent> events,
            StationEventType type,
            String processKey,
            String catalogItemId
    ) {
        return events.stream()
                .filter(event -> type == event.getEventType())
                .filter(event -> matchesProcessIdentity(event, processKey, catalogItemId))
                .map(StationOperationEvent::getUnits)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public static int sumDeliveryUnits(List<StationOperationEvent> events, String processKey) {
        return sumDeliveryUnits(events, processKey, null);
    }

    public static int sumDeliveryUnits(
            List<StationOperationEvent> events,
            String processKey,
            String catalogItemId
    ) {
        return events.stream()
                .filter(event -> matchesProcessIdentity(event, processKey, catalogItemId))
                .filter(event -> event.getEventType() == StationEventType.ENTREGA_PARCIAL
                        || event.getEventType() == StationEventType.ENTREGA_TOTAL)
                .map(StationOperationEvent::getUnits)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public static boolean hasOpenPause(
            List<StationOperationEvent> events,
            String processKey,
            String userId
    ) {
        return hasOpenPause(events, processKey, userId, null);
    }

    public static boolean hasOpenPause(
            List<StationOperationEvent> events,
            String processKey,
            String userId,
            String catalogItemId
    ) {
        boolean paused = false;
        for (StationOperationEvent event : events) {
            if (!userId.equals(event.getUserId())
                    || !matchesProcessIdentity(event, processKey, catalogItemId)) {
                continue;
            }
            if (event.getEventType() == StationEventType.PARO) {
                paused = true;
            } else if (event.getEventType() == StationEventType.REANUDACION) {
                paused = false;
            }
        }
        return paused;
    }

    /** Coincide por processKey exacto o, en ítems de catálogo, por catalogItemId (aliases legacy). */
    private static boolean matchesProcessIdentity(
            StationOperationEvent event,
            String processKey,
            String catalogItemId
    ) {
        if (processKey != null && processKey.equals(event.getProcessKey())) {
            return true;
        }
        if (catalogItemId == null
                || catalogItemId.isBlank()
                || !catalogItemId.equals(event.getCatalogItemId())) {
            return false;
        }
        String eventKey = event.getProcessKey();
        if (eventKey == null || processKey == null) {
            return false;
        }
        boolean bothTerminado = processKey.startsWith("terminado:") && eventKey.startsWith("terminado:");
        boolean bothAcabado = processKey.startsWith("acabado:") && eventKey.startsWith("acabado:");
        return bothTerminado || bothAcabado;
    }
}
