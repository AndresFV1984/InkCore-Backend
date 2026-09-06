package com.inkcore.infrastructure.in.rest.station;

import com.inkcore.domain.station.model.StationEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public final class StationRequests {

    private StationRequests() {
    }

    @Schema(
            name = "StationEventRequest",
            description = """
                    Cuerpo para registrar eventos de estación.
                    El backend completa actorUserId/actorName (JWT), clientId (desde la OP) e IDs.
                    Roles: OPERADOR o ADMINISTRADOR.
                    """
    )
    public record EventRequest(
            @Schema(
                    description = "ID de la OP. Obligatorio salvo eventos de jornada (isShiftEvent=true).",
                    example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd"
            )
            String productionOrderId,
            @Schema(description = "Nombre del trabajo (snapshot opcional)", example = "cuaderno")
            String workName,
            @Schema(
                    description = "Fase operativa en kebab-case español",
                    example = "preprensa",
                    allowableValues = {
                            "preprensa", "corte-papel", "impresion", "terminados", "acabados", "cobro", "jornada"
                    }
            )
            @NotBlank String phase,
            @Schema(
                    description = """
                            Clave de proceso: nombre de fase (`preprensa`), `jornada`,
                            o ítem de catálogo (`terminado:{catalogItemId}`, `acabado:{catalogItemId}`).
                            """,
                    example = "preprensa"
            )
            @NotBlank String processKey,
            @Schema(
                    description = "Operario del proceso. Si se omite, se usa el userId del JWT. Debe estar asignado a la fase en la OP.",
                    example = "operator-seed-003"
            )
            String userId,
            @Schema(description = "Unidades (obligatorio en advance y delivery/*)", example = "250")
            @Positive Integer units,
            @Schema(description = "Snapshot del estado de producción en planta", example = "En Proceso")
            String productionStatus,
            @Schema(description = "Nota libre del operario")
            String note,
            @Schema(
                    description = "Motivo de pausa (obligatorio en pause y shift-mark). Valores: problema_maquina, calidad, insumos_pendientes, espera_material, cambio_trabajo, apoyo_otra_orden, instruccion_supervisor, capacitacion, descanso_almuerzo, descanso_desayuno, descanso_general, otro, inicio_horario, fin_horario, inicio_operacion, fin_operacion.",
                    example = "problema_maquina"
            )
            String pauseReason,
            @Schema(
                    description = "Fecha/hora local del hecho (LocalDateTime ISO-8601 sin zona). No enviar sufijo Z. Si se omite, usa ahora del servidor.",
                    example = "2026-09-02T17:01:30",
                    type = "string",
                    format = "date-time"
            )
            LocalDateTime occurredAt,
            @Schema(description = "True para marca de jornada sin OP (shift-mark)", example = "false")
            Boolean isShiftEvent,
            @Schema(
                    description = "Alias de pauseReason para marca de jornada (inicio_horario | fin_horario)",
                    example = "inicio_horario"
            )
            String reason
    ) {
    }

    public static RegisterStationEventPayload toAdvance(EventRequest request) {
        return new RegisterStationEventPayload(
                request.productionOrderId(),
                request.workName(),
                request.phase(),
                request.processKey(),
                request.userId(),
                request.units(),
                request.productionStatus(),
                request.note(),
                null,
                request.occurredAt(),
                false,
                StationEventType.AVANCE_UNIDADES
        );
    }

    public static RegisterStationEventPayload toPause(EventRequest request) {
        return new RegisterStationEventPayload(
                request.productionOrderId(),
                request.workName(),
                request.phase(),
                request.processKey(),
                request.userId(),
                null,
                request.productionStatus(),
                request.note(),
                request.pauseReason(),
                request.occurredAt(),
                false,
                StationEventType.PARO
        );
    }

    public static RegisterStationEventPayload toResume(EventRequest request) {
        return new RegisterStationEventPayload(
                request.productionOrderId(),
                request.workName(),
                request.phase(),
                request.processKey(),
                request.userId(),
                null,
                request.productionStatus(),
                request.note(),
                null,
                request.occurredAt(),
                false,
                StationEventType.REANUDACION
        );
    }

    public static RegisterStationEventPayload toPartialDelivery(EventRequest request) {
        return new RegisterStationEventPayload(
                request.productionOrderId(),
                request.workName(),
                request.phase(),
                request.processKey(),
                request.userId(),
                request.units(),
                request.productionStatus(),
                request.note(),
                null,
                request.occurredAt(),
                false,
                StationEventType.ENTREGA_PARCIAL
        );
    }

    public static RegisterStationEventPayload toTotalDelivery(EventRequest request) {
        return new RegisterStationEventPayload(
                request.productionOrderId(),
                request.workName(),
                request.phase(),
                request.processKey(),
                request.userId(),
                request.units(),
                request.productionStatus(),
                request.note(),
                null,
                request.occurredAt(),
                false,
                StationEventType.ENTREGA_TOTAL
        );
    }

    public static RegisterStationEventPayload toShiftMark(EventRequest request) {
        return new RegisterStationEventPayload(
                request.productionOrderId(),
                request.workName(),
                request.phase() == null ? "jornada" : request.phase(),
                request.processKey() == null ? "jornada" : request.processKey(),
                request.userId(),
                null,
                request.productionStatus(),
                request.note(),
                request.reason() != null ? request.reason() : request.pauseReason(),
                request.occurredAt(),
                request.isShiftEvent() == null || request.isShiftEvent(),
                StationEventType.MARCA_HORARIO
        );
    }

    public static RegisterStationEventPayload toPhaseStart(EventRequest request) {
        return new RegisterStationEventPayload(
                request.productionOrderId(),
                request.workName(),
                request.phase(),
                request.processKey(),
                request.userId(),
                null,
                request.productionStatus(),
                request.note(),
                null,
                request.occurredAt(),
                false,
                StationEventType.INICIO_FASE
        );
    }

    public static RegisterStationEventPayload toPhaseEnd(EventRequest request) {
        return new RegisterStationEventPayload(
                request.productionOrderId(),
                request.workName(),
                request.phase(),
                request.processKey(),
                request.userId(),
                null,
                request.productionStatus(),
                request.note(),
                null,
                request.occurredAt(),
                false,
                StationEventType.FIN_FASE
        );
    }

    public record RegisterStationEventPayload(
            String productionOrderId,
            String workName,
            String phase,
            String processKey,
            String userId,
            Integer units,
            String productionStatus,
            String note,
            String pauseReason,
            LocalDateTime occurredAt,
            boolean shiftEvent,
            StationEventType eventType
    ) {
    }
}
