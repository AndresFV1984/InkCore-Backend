package com.inkcore.domain.station.model;

import java.util.Arrays;
import java.util.Locale;

public enum StationProcessStatus {

    PENDIENTE("pendiente"),
    EN_PROCESO("en-proceso"),
    TERMINADO("terminado");

    private final String dbValue;

    StationProcessStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static StationProcessStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return PENDIENTE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(status -> status.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado de proceso inválido: " + value));
    }
}
