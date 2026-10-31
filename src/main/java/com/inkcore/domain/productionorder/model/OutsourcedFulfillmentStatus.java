package com.inkcore.domain.productionorder.model;

/**
 * Ciclo operativo de una etapa tercerizada.
 * PENDING → DISPATCHED → IN_PROGRESS → COMPLETED.
 * Compatibilidad: QUOTED→PENDING, SENT→IN_PROGRESS, RECEIVED→COMPLETED.
 */
public enum OutsourcedFulfillmentStatus {
    PENDING,
    DISPATCHED,
    IN_PROGRESS,
    COMPLETED;

    public static OutsourcedFulfillmentStatus fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return PENDING;
        }
        String key = raw.trim().toUpperCase();
        return switch (key) {
            case "QUOTED" -> PENDING;
            case "SENT" -> IN_PROGRESS;
            case "RECEIVED" -> COMPLETED;
            default -> OutsourcedFulfillmentStatus.valueOf(key);
        };
    }

    public static OutsourcedFulfillmentStatus resolve(
            OutsourcedFulfillmentStatus explicit,
            java.time.LocalDate receivedAt,
            java.time.LocalDate sentAt
    ) {
        if (explicit != null) {
            return explicit;
        }
        if (receivedAt != null) {
            return COMPLETED;
        }
        if (sentAt != null) {
            return IN_PROGRESS;
        }
        return PENDING;
    }
}
