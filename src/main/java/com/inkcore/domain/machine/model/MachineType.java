package com.inkcore.domain.machine.model;

import java.util.Arrays;
import java.util.Locale;

/**
 * Fase del wizard a la que pertenece una máquina. Mismo dominio que
 * {@code station_operation_events.phase}, sin cobro ni jornada.
 */
public enum MachineType {

    PREPRENSA("preprensa"),
    CORTE_PAPEL("corte-papel"),
    IMPRESION("impresion"),
    TERMINADOS("terminados"),
    ACABADOS("acabados");

    private final String apiValue;

    MachineType(String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }

    public static MachineType fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El tipo de máquina es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.apiValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "El tipo de máquina debe ser preprensa, corte-papel, impresion, terminados o acabados"));
    }
}
