package com.inkcore.domain.station.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationValidationServiceTest {

    @Test
    void isInProgressStatus_acceptsGenericAndPhaseStatuses() {
        assertTrue(StationValidationService.isInProgressStatus("IN_PROGRESS"));
        assertTrue(StationValidationService.isInProgressStatus("EN PROCESO"));
        assertTrue(StationValidationService.isInProgressStatus("IN_PROGRESS_PREPRESS"));
        assertTrue(StationValidationService.isInProgressStatus("IN_PROGRESS_CUTTING"));
        assertTrue(StationValidationService.isInProgressStatus("IN_PROGRESS_PRINTING"));
        assertTrue(StationValidationService.isInProgressStatus("IN_PROGRESS_FINISHED_PRODUCTS"));
        assertTrue(StationValidationService.isInProgressStatus("IN_PROGRESS_FINISHING"));
        assertTrue(StationValidationService.isInProgressStatus("in_progress_prepress"));
        assertTrue(StationValidationService.isInProgressStatus("En Proceso Impresion"));
        assertTrue(StationValidationService.isInProgressStatus("EN PROCESO PREPRENSA"));
        assertTrue(StationValidationService.isInProgressStatus("En Proceso Terminados"));
        assertTrue(StationValidationService.isInProgressStatus("En Proceso · Impresión"));
        assertEquals(
                "IN_PROGRESS_PRINTING",
                StationValidationService.toCanonicalInProgressWire("En Proceso Impresion")
        );
        assertEquals(
                "IN_PROGRESS",
                StationValidationService.toCanonicalInProgressWire("EN PROCESO")
        );
    }

    @Test
    void isInProgressStatus_rejectsNonPlantStatuses() {
        assertFalse(StationValidationService.isInProgressStatus(null));
        assertFalse(StationValidationService.isInProgressStatus(""));
        assertFalse(StationValidationService.isInProgressStatus("PENDING"));
        assertFalse(StationValidationService.isInProgressStatus("PAUSED"));
        assertFalse(StationValidationService.isInProgressStatus("UNDER_REVIEW"));
        assertFalse(StationValidationService.isInProgressStatus("COMPLETED"));
        assertFalse(StationValidationService.isInProgressStatus("ANULADA"));
        assertFalse(StationValidationService.isInProgressStatus("CANCELLED"));
    }
}
