package com.inkcore.domain.station.model;

import java.util.Arrays;
import java.util.Locale;

public enum StationCatalogItemKind {

    TERMINADO("terminado"),
    ACABADO("acabado");

    private final String dbValue;

    StationCatalogItemKind(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static StationCatalogItemKind fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(kind -> kind.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de ítem de catálogo inválido: " + value));
    }
}
