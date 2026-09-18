package com.inkcore.domain.productionorder.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionOrderStatusTest {

    @Test
    void fromWire_acceptsCanonicalAnulada() {
        assertEquals(ProductionOrderStatus.ANULADA, ProductionOrderStatus.fromWire("ANULADA"));
        assertEquals(ProductionOrderStatus.ANULADA, ProductionOrderStatus.fromWire("anulada"));
    }

    @Test
    void fromWire_normalizesCancelledAliasesToAnulada() {
        assertEquals(ProductionOrderStatus.ANULADA, ProductionOrderStatus.fromWire("CANCELLED"));
        assertEquals(ProductionOrderStatus.ANULADA, ProductionOrderStatus.fromWire("CANCELED"));
        assertEquals(ProductionOrderStatus.ANULADA, ProductionOrderStatus.fromWire("CANCELADA"));
        assertEquals(ProductionOrderStatus.ANULADA, ProductionOrderStatus.fromWire("ANULADO"));
        assertEquals(ProductionOrderStatus.ANULADA, ProductionOrderStatus.fromWire(" cancelled "));
    }

    @Test
    void fromWire_acceptsOtherPlantStatuses() {
        assertEquals(ProductionOrderStatus.PENDING, ProductionOrderStatus.fromWire("PENDING"));
        assertEquals(ProductionOrderStatus.IN_PROGRESS, ProductionOrderStatus.fromWire("in_progress"));
        assertEquals(ProductionOrderStatus.COMPLETED, ProductionOrderStatus.fromWire("COMPLETED"));
    }

    @Test
    void fromWire_rejectsUnknownStatus() {
        assertThrows(IllegalArgumentException.class, () -> ProductionOrderStatus.fromWire("UNKNOWN"));
    }

    @Test
    void toWire_alwaysReturnsAnuladaForAliases() {
        assertEquals("ANULADA", ProductionOrderStatus.toWire("CANCELLED"));
        assertEquals("ANULADA", ProductionOrderStatus.toWire("ANULADA"));
        assertEquals("IN_PROGRESS", ProductionOrderStatus.toWire("IN_PROGRESS"));
    }

    @Test
    void normalizeFilter_mapsCancelledToAnulada() {
        assertEquals("ANULADA", ProductionOrderStatus.normalizeFilter("CANCELLED"));
        assertEquals("PENDING", ProductionOrderStatus.normalizeFilter("pending"));
        assertEquals(null, ProductionOrderStatus.normalizeFilter("  "));
    }

    @Test
    void isInProgress_detectsPlantProgressStates() {
        assertEquals(true, ProductionOrderStatus.IN_PROGRESS.isInProgress());
        assertEquals(true, ProductionOrderStatus.IN_PROGRESS_PREPRESS.isInProgress());
        assertEquals(false, ProductionOrderStatus.PENDING.isInProgress());
        assertEquals(false, ProductionOrderStatus.COMPLETED.isInProgress());
        assertEquals(true, ProductionOrderStatus.isInProgressWire("IN_PROGRESS_CUTTING"));
        assertEquals(false, ProductionOrderStatus.isInProgressWire("PENDING"));
    }
}
