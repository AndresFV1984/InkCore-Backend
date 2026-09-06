package com.inkcore.domain.station.model;

import com.inkcore.domain.productionorder.model.ProductionOrderStage;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Fases del wizard de Estación (API en kebab-case español).
 */
public enum StationPhase {

    PREPRENSA("preprensa", ProductionOrderStage.PREPRESS),
    CORTE_PAPEL("corte-papel", ProductionOrderStage.CUTTING),
    IMPRESION("impresion", ProductionOrderStage.PRINTING),
    TERMINADOS("terminados", ProductionOrderStage.FINISHED_PRODUCTS),
    ACABADOS("acabados", ProductionOrderStage.FINISHING_PROCESSES),
    COBRO("cobro", ProductionOrderStage.BILLING),
    JORNADA("jornada", null);

    private final String apiValue;
    private final ProductionOrderStage orderStage;

    StationPhase(String apiValue, ProductionOrderStage orderStage) {
        this.apiValue = apiValue;
        this.orderStage = orderStage;
    }

    public String getApiValue() {
        return apiValue;
    }

    public Optional<ProductionOrderStage> getOrderStage() {
        return Optional.ofNullable(orderStage);
    }

    public static StationPhase fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La fase es obligatoria");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(phase -> phase.apiValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Fase inválida: " + value));
    }
}
