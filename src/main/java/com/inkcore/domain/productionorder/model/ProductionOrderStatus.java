package com.inkcore.domain.productionorder.model;

import java.util.Locale;
import java.util.Set;

/**
 * Estado de planta de una Orden de Producción ({@code production_orders.status}).
 * <p>
 * Valor canónico de cierre/anulación: {@link #ANULADA} (reemplaza {@code CANCELLED}).
 * Distinto de CxC {@code accounts_receivable.status = anulado}.
 */
public enum ProductionOrderStatus {

    PENDING,
    PAUSED,
    UNDER_REVIEW,
    IN_PROGRESS,
    IN_PROGRESS_PREPRESS,
    IN_PROGRESS_CUTTING,
    IN_PROGRESS_PRINTING,
    IN_PROGRESS_FINISHED_PRODUCTS,
    IN_PROGRESS_FINISHING,
    COMPLETED,
    ANULADA;

    private static final Set<String> ANULADA_ALIASES = Set.of(
            "ANULADA",
            "ANULADO",
            "CANCELLED",
            "CANCELED",
            "CANCELADA"
    );

    public String getWireValue() {
        return name();
    }

    /**
     * Normaliza entrada de API/BD al valor canónico. Acepta aliases temporales de anulación.
     */
    public static ProductionOrderStatus fromWire(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("status de planta es obligatorio");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (ANULADA_ALIASES.contains(normalized)) {
            return ANULADA;
        }
        for (ProductionOrderStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("status de planta inválido: " + raw);
    }

    /**
     * Valor a exponer en respuestas REST. Aliases de anulación → {@code ANULADA}.
     * Valores desconocidos se devuelven en mayúsculas sin fallar (datos legacy).
     */
    public static String toWire(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (ANULADA_ALIASES.contains(normalized)) {
            return ANULADA.getWireValue();
        }
        return normalized;
    }

    /**
     * Normaliza filtro de listado: aliases de anulación → {@code ANULADA}.
     * Otros valores se dejan en mayúsculas para igualdad exacta en BD.
     */
    public static String normalizeFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (ANULADA_ALIASES.contains(normalized)) {
            return ANULADA.getWireValue();
        }
        return normalized;
    }

    public static boolean isAnuladaAlias(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return ANULADA_ALIASES.contains(raw.trim().toUpperCase(Locale.ROOT));
    }

    /** True para {@code IN_PROGRESS} y variantes de estación ({@code IN_PROGRESS_*}). */
    public boolean isInProgress() {
        return this == IN_PROGRESS || name().startsWith("IN_PROGRESS_");
    }

    public static boolean isInProgressWire(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("IN_PROGRESS") || normalized.startsWith("IN_PROGRESS_");
    }
}
