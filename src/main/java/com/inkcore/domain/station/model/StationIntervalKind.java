package com.inkcore.domain.station.model;

import java.util.Arrays;
import java.util.Locale;

public enum StationIntervalKind {

    LABOR("labor"),
    PAUSE("pause"),
    SHIFT("shift");

    private final String dbValue;

    StationIntervalKind(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static StationIntervalKind fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El tipo de intervalo es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(kind -> kind.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de intervalo inválido: " + value));
    }
}
