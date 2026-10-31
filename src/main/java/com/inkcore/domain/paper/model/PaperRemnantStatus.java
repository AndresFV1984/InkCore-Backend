package com.inkcore.domain.paper.model;

import com.inkcore.domain.productionorder.model.ProductionOrderStatus;

import java.util.Locale;

/**
 * Estado de un remanente de papel.
 * <p>
 * Alineado con {@link ProductionOrderStatus} de la OP origen, salvo
 * {@link ProductionOrderStatus#COMPLETED} → {@link #AVAILABLE} (Disponible).
 * {@link #INACTIVE} es solo para baja manual del administrador.
 */
public enum PaperRemnantStatus {

    PENDING,
    PAUSED,
    UNDER_REVIEW,
    IN_PROGRESS,
    IN_PROGRESS_PREPRESS,
    IN_PROGRESS_CUTTING,
    IN_PROGRESS_PRINTING,
    IN_PROGRESS_FINISHED_PRODUCTS,
    IN_PROGRESS_FINISHING,
    /** Equivalente a OP Finalizada: listo para reutilizar en otras órdenes. */
    AVAILABLE,
    ANULADA,
    /** Baja manual (antes {@code state=false}); no usable en corte. */
    INACTIVE;

    public String getWireValue() {
        return name();
    }

    public boolean isUsableInCutting() {
        return this == AVAILABLE;
    }

    /** Compatibilidad con el boolean legacy {@code state}. */
    public boolean toLegacyState() {
        return this == AVAILABLE;
    }

    public static PaperRemnantStatus fromWire(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("status del remanente es obligatorio");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("COMPLETED".equals(normalized) || "FINALIZADA".equals(normalized)) {
            return AVAILABLE;
        }
        if ("ACTIVE".equals(normalized) || "TRUE".equals(normalized) || "DISPONIBLE".equals(normalized)) {
            return AVAILABLE;
        }
        if ("FALSE".equals(normalized) || "INACTIVE".equals(normalized) || "INACTIVO".equals(normalized)) {
            return INACTIVE;
        }
        if (ProductionOrderStatus.isAnuladaAlias(normalized)) {
            return ANULADA;
        }
        for (PaperRemnantStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("status de remanente inválido: " + raw);
    }

    /**
     * Mapea el estado de la OP origen al del remanente.
     * Finalizada ({@code COMPLETED}) → {@link #AVAILABLE}.
     */
    public static PaperRemnantStatus fromProductionOrderStatus(ProductionOrderStatus orderStatus) {
        if (orderStatus == null) {
            return AVAILABLE;
        }
        if (orderStatus == ProductionOrderStatus.COMPLETED) {
            return AVAILABLE;
        }
        return fromWire(orderStatus.getWireValue());
    }

    public static PaperRemnantStatus fromProductionOrderStatusWire(String orderStatusWire) {
        if (orderStatusWire == null || orderStatusWire.isBlank()) {
            return AVAILABLE;
        }
        return fromProductionOrderStatus(ProductionOrderStatus.fromWire(orderStatusWire));
    }

    public static PaperRemnantStatus fromLegacyState(Boolean state) {
        if (state == null || state) {
            return AVAILABLE;
        }
        return INACTIVE;
    }
}
