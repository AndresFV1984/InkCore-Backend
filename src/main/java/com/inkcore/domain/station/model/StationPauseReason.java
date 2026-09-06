package com.inkcore.domain.station.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public enum StationPauseReason {

    INICIO_OPERACION("inicio_operacion"),
    FIN_OPERACION("fin_operacion"),
    INICIO_HORARIO("inicio_horario"),
    FIN_HORARIO("fin_horario"),
    PROBLEMA_MAQUINA("problema_maquina"),
    CALIDAD("calidad"),
    INSUMOS_PENDIENTES("insumos_pendientes"),
    ESPERA_MATERIAL("espera_material"),
    CAMBIO_TRABAJO("cambio_trabajo"),
    APOYO_OTRA_ORDEN("apoyo_otra_orden"),
    INSTRUCCION_SUPERVISOR("instruccion_supervisor"),
    CAPACITACION("capacitacion"),
    DESCANSO_ALMUERZO("descanso_almuerzo"),
    DESCANSO_DESAYUNO("descanso_desayuno"),
    DESCANSO_GENERAL("descanso_general"),
    OTRO("otro");

    private static final Set<StationPauseReason> NON_BLOCKING = Set.of(
            INICIO_HORARIO, FIN_HORARIO, INICIO_OPERACION, FIN_OPERACION
    );

    private final String dbValue;

    StationPauseReason(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public boolean isNonBlocking() {
        return NON_BLOCKING.contains(this);
    }

    public static StationPauseReason fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El motivo de pausa es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(reason -> reason.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Motivo de pausa inválido: " + value));
    }
}
