package com.inkcore.infrastructure.in.rest.station;

import com.inkcore.application.station.usecase.GetStationActiveSessionUseCase;
import com.inkcore.application.station.usecase.GetStationBitacoraUseCase;
import com.inkcore.application.station.usecase.GetStationOrderDetailUseCase;
import com.inkcore.application.station.usecase.GetStationLaborSettlementUseCase;
import com.inkcore.application.station.usecase.GetStationTraceReportUseCase;
import com.inkcore.application.station.usecase.ListStationInboxUseCase;
import com.inkcore.application.station.usecase.ListStationOrderProcessesUseCase;
import com.inkcore.domain.productionorder.model.OperatorAssignment;
import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.station.model.StationOperationEvent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public final class StationResponses {

    private StationResponses() {
    }

    @Schema(name = "StationEventResponse", description = "Evento registrado en estación (actor* lo completa el backend)")
    public record EventResponse(
            @Schema(example = "d0a4d094-0f70-48f7-bafc-47f91f7c1eaa") String id,
            @Schema(example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd") String productionOrderId,
            @Schema(description = "Alias de productionOrderId", example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd") String orderId,
            @Schema(example = "cuaderno") String workName,
            @Schema(example = "preprensa") String phase,
            @Schema(example = "preprensa") String processKey,
            String catalogItemId,
            String catalogItemLabel,
            @Schema(example = "operator-seed-003") String userId,
            @Schema(
                    description = "Tipo de evento persistido",
                    example = "paro",
                    allowableValues = {
                            "asignacion", "cambio_estado_orden", "entrega_parcial", "entrega_total",
                            "avance_unidades", "marca_horario", "inicio_fase", "fin_fase", "paro", "reanudacion"
                    }
            ) String type,
            @Schema(description = "occurredAt del evento (sin zona)", example = "2026-09-02T17:01:30") String at,
            @Schema(example = "250") Integer unidades,
            @Schema(description = "Usuario del JWT que ejecutó la acción") String actorUserId,
            String actorName,
            String productionStatus,
            @Schema(description = "Motivo de pausa (paro / marca_horario)", example = "cambio_trabajo")
            String pauseReason,
            @Schema(description = "Nota libre del operario", example = "texto de la nota")
            String note
    ) {
        public static EventResponse from(StationOperationEvent event) {
            return new EventResponse(
                    event.getEventId(),
                    event.getProductionOrderId(),
                    event.getProductionOrderId(),
                    event.getWorkName(),
                    event.getPhase(),
                    event.getProcessKey(),
                    event.getCatalogItemId(),
                    event.getCatalogItemLabel(),
                    event.getUserId(),
                    event.getEventType().getDbValue(),
                    event.getOccurredAt() == null ? null : event.getOccurredAt().toString(),
                    event.getUnits(),
                    event.getActorUserId(),
                    event.getActorName(),
                    event.getProductionStatusSnapshot(),
                    event.getPauseReason(),
                    event.getNote()
            );
        }
    }

    @Schema(name = "StationInboxItemResponse", description = "Fila del inbox de estación")
    public record InboxItemResponse(
            @Schema(example = "po-uuid-001") String productionOrderId,
            @Schema(example = "OP-42") String displayNumber,
            @Schema(example = "Volantes A5") String workName,
            String clientId,
            String clientName,
            String designName,
            @Schema(example = "En Proceso") String productionStatus,
            boolean executionAllowed,
            AssignedProcessSummaryResponse assignedProcessSummary,
            @Schema(description = "Unidades disponibles para pedidos (agregado station)", example = "1500")
            int cantidadDisponible
    ) {
        public static InboxItemResponse from(ListStationInboxUseCase.StationInboxItem item) {
            return new InboxItemResponse(
                    item.productionOrderId(),
                    item.displayNumber(),
                    item.workName(),
                    item.clientId(),
                    item.clientName(),
                    item.designName(),
                    item.productionStatus(),
                    item.executionAllowed(),
                    AssignedProcessSummaryResponse.from(item.assignedProcessSummary()),
                    item.cantidadDisponible()
            );
        }
    }

    @Schema(name = "StationAssignedProcessSummary")
    public record AssignedProcessSummaryResponse(int done, int active, int pending, int progressPct) {
        static AssignedProcessSummaryResponse from(ListStationInboxUseCase.AssignedProcessSummary summary) {
            return new AssignedProcessSummaryResponse(
                    summary.done(), summary.active(), summary.pending(), summary.progressPct());
        }
    }

    @Schema(name = "StationOrderDetailResponse", description = "Detalle de OP para estación")
    public record OrderDetailResponse(
            String productionOrderId,
            String displayNumber,
            String workName,
            String clientId,
            String clientName,
            String designName,
            String productionStatus,
            boolean executionAllowed,
            int requestedQuantity,
            List<OperatorAssignmentResponse> operators,
            List<PostpressRecordResponse> postpressRecords
    ) {
        public static OrderDetailResponse from(GetStationOrderDetailUseCase.StationOrderDetail detail) {
            return new OrderDetailResponse(
                    detail.productionOrderId(),
                    detail.displayNumber(),
                    detail.workName(),
                    detail.clientId(),
                    detail.clientName(),
                    detail.designName(),
                    detail.productionStatus(),
                    detail.executionAllowed(),
                    detail.requestedQuantity(),
                    detail.operators().stream().map(OperatorAssignmentResponse::from).toList(),
                    detail.postpressRecords().stream().map(PostpressRecordResponse::from).toList()
            );
        }
    }

    public record OperatorAssignmentResponse(String stage, String userId, String roleCode) {
        static OperatorAssignmentResponse from(OperatorAssignment assignment) {
            return new OperatorAssignmentResponse(
                    assignment.getStage().name(),
                    assignment.getUserId(),
                    assignment.getRoleCode()
            );
        }
    }

    public record PostpressRecordResponse(
            String productionOrderPostpressRecordId,
            String plateId,
            String type,
            Boolean completed,
            List<PostpressLineResponse> lines
    ) {
        static PostpressRecordResponse from(PostpressRecord record) {
            return new PostpressRecordResponse(
                    record.getRecordId(),
                    record.getPlateId(),
                    record.getType() == null ? null : record.getType().name(),
                    record.isCompleted(),
                    record.getLines().stream().map(PostpressLineResponse::from).toList()
            );
        }
    }

    public record PostpressLineResponse(
            String productionOrderPostpressLineId,
            String catalogItemId,
            String itemName
    ) {
        static PostpressLineResponse from(PostpressLine line) {
            return new PostpressLineResponse(line.getLineId(), line.getCatalogItemId(), line.getItemName());
        }
    }

    @Schema(name = "StationProcessRowResponse", description = "Fila de proceso calculada para una OP")
    public record ProcessRowResponse(
            String phase,
            String processKey,
            String catalogItemId,
            String catalogItemLabel,
            String userId,
            int totalUnits,
            int completedUnits,
            int deliveredUnits,
            String status,
            long laborTimeMs,
            long pausedTimeMs
    ) {
        public static ProcessRowResponse from(ListStationOrderProcessesUseCase.StationProcessRow row) {
            return new ProcessRowResponse(
                    row.phase(),
                    row.processKey(),
                    row.catalogItemId(),
                    row.catalogItemLabel(),
                    row.userId(),
                    row.totalUnits(),
                    row.completedUnits(),
                    row.deliveredUnits(),
                    row.status(),
                    row.laborTimeMs(),
                    row.pausedTimeMs()
            );
        }
    }

    @Schema(name = "StationBitacoraResponse", description = "Resumen de bitácora de un proceso (entries paginadas)")
    public record BitacoraResponse(
            String processKey,
            String catalogItemId,
            String catalogItemLabel,
            long laborTimeMs,
            long pausedTimeMs,
            int pauseCount,
            boolean isPausedNow,
            List<BitacoraEntryResponse> entries,
            List<BitacoraPauseResponse> pauses,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
        public static BitacoraResponse from(GetStationBitacoraUseCase.BitacoraSummary summary) {
            return new BitacoraResponse(
                    summary.processKey(),
                    summary.catalogItemId(),
                    summary.catalogItemLabel(),
                    summary.laborTimeMs(),
                    summary.pausedTimeMs(),
                    summary.pauseCount(),
                    summary.isPausedNow(),
                    summary.entries().stream().map(BitacoraEntryResponse::from).toList(),
                    summary.pauses().stream().map(BitacoraPauseResponse::from).toList(),
                    summary.page(),
                    summary.size(),
                    summary.totalElements(),
                    summary.totalPages(),
                    summary.hasNext()
            );
        }
    }

    public record BitacoraEntryResponse(String id, String type, String at, Integer units, String note) {
        static BitacoraEntryResponse from(GetStationBitacoraUseCase.BitacoraEntry entry) {
            return new BitacoraEntryResponse(entry.id(), entry.type(), entry.at(), entry.units(), entry.note());
        }
    }

    public record BitacoraPauseResponse(String id, String reason, String at, String note) {
        static BitacoraPauseResponse from(GetStationBitacoraUseCase.BitacoraPause pause) {
            return new BitacoraPauseResponse(pause.id(), pause.reason(), pause.at(), pause.note());
        }
    }

    @Schema(name = "StationActiveSessionResponse", description = "Intervalo laboral o de pausa abierto del operario")
    public record ActiveSessionResponse(
            String intervalId,
            String productionOrderId,
            String processKey,
            String phase,
            String intervalKind,
            String startedAt,
            boolean open
    ) {
        public static ActiveSessionResponse from(GetStationActiveSessionUseCase.ActiveSession session) {
            return new ActiveSessionResponse(
                    session.intervalId(),
                    session.productionOrderId(),
                    session.processKey(),
                    session.phase(),
                    session.intervalKind(),
                    session.startedAt(),
                    session.open()
            );
        }
    }
}
