package com.inkcore.application.station;

import com.inkcore.application.station.usecase.ListStationOrderProcessesUseCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationOrderCompletionServiceTest {

    private static ListStationOrderProcessesUseCase.StationProcessRow row(
            String processKey,
            int total,
            int completed,
            int delivered,
            String status
    ) {
        return new ListStationOrderProcessesUseCase.StationProcessRow(
                processKey,
                processKey,
                null,
                null,
                "op-1",
                total,
                completed,
                delivered,
                status,
                0L,
                0L
        );
    }

    @Test
    void allStagesFullyProcessed_requiresEveryPositiveCupo() {
        assertTrue(StationOrderCompletionService.allStagesFullyProcessed(List.of(
                row("preprensa", 1000, 1000, 1000, "terminado"),
                row("corte-papel", 1000, 1000, 1000, "terminado"),
                row("impresion", 1000, 1000, 1000, "terminado")
        )));
        assertFalse(StationOrderCompletionService.allStagesFullyProcessed(List.of(
                row("preprensa", 1000, 1000, 1000, "terminado"),
                row("corte-papel", 1000, 500, 0, "en-proceso")
        )));
        assertFalse(StationOrderCompletionService.allStagesFullyProcessed(List.of()));
    }

    @Test
    void allStagesFullyProcessed_acceptsDeliveredWithoutFullAdvance() {
        assertTrue(StationOrderCompletionService.allStagesFullyProcessed(List.of(
                row("preprensa", 1000, 0, 1000, "terminado"),
                row("impresion", 1000, 400, 1000, "terminado")
        )));
    }

    @Test
    void allStagesFullyProcessed_ignoresMissingOptionalPhases() {
        // OP sin Terminados/Acabados: solo las etapas reales deben cumplirse.
        assertTrue(StationOrderCompletionService.allStagesFullyProcessed(List.of(
                row("preprensa", 500, 500, 500, "terminado"),
                row("corte-papel", 500, 500, 500, "terminado"),
                row("impresion", 500, 500, 500, "terminado")
        )));
    }
}
