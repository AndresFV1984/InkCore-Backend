package com.inkcore.domain.station.model;

import java.util.Arrays;
import java.util.Locale;

public enum StationEventType {

    ASIGNACION("asignacion"),
    CAMBIO_ESTADO_ORDEN("cambio_estado_orden"),
    ENTREGA_PARCIAL("entrega_parcial"),
    ENTREGA_TOTAL("entrega_total"),
    AVANCE_UNIDADES("avance_unidades"),
    MARCA_HORARIO("marca_horario"),
    INICIO_FASE("inicio_fase"),
    FIN_FASE("fin_fase"),
    PARO("paro"),
    REANUDACION("reanudacion");

    private final String dbValue;

    StationEventType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static StationEventType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El tipo de evento es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de evento inválido: " + value));
    }
}
