package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationIntervalService;
import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PostpressType;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationOperationInterval;
import com.inkcore.domain.station.model.StationPhase;
import com.inkcore.domain.station.model.StationProcessProgress;
import com.inkcore.domain.station.model.StationProcessStatus;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import com.inkcore.domain.station.ports.out.StationOperationIntervalRepositoryPort;
import com.inkcore.domain.station.ports.out.StationProcessProgressRepositoryPort;
import com.inkcore.domain.station.service.StationProcessKeyResolver;
import com.inkcore.domain.station.service.StationValidationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ListStationOrderProcessesUseCase {

    private final StationSupport support;
    private final StationProcessProgressRepositoryPort progressRepository;
    private final StationOperationEventRepositoryPort eventRepository;
    private final StationOperationIntervalRepositoryPort intervalRepository;
    private final StationIntervalService intervalService;

    public ListStationOrderProcessesUseCase(
            StationSupport support,
            StationProcessProgressRepositoryPort progressRepository,
            StationOperationEventRepositoryPort eventRepository,
            StationOperationIntervalRepositoryPort intervalRepository,
            StationIntervalService intervalService
    ) {
        this.support = support;
        this.progressRepository = progressRepository;
        this.eventRepository = eventRepository;
        this.intervalRepository = intervalRepository;
        this.intervalService = intervalService;
    }

    @Transactional(readOnly = true)
    public List<StationProcessRow> execute(String productionOrderId, Authentication authentication) {
        return executeInternal(productionOrderId, support.companyId(authentication));
    }

    @Transactional(readOnly = true)
    public List<StationProcessRow> executeInternal(String productionOrderId, String companyId) {
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        List<StationOperationEvent> events =
                eventRepository.findAllByProductionOrderId(companyId, productionOrderId);
        List<StationProcessProgress> stored =
                progressRepository.findByProductionOrderId(companyId, productionOrderId);

        List<StationProcessRow> rows = new ArrayList<>();
        for (StationPhase phase : List.of(
                StationPhase.PREPRENSA,
                StationPhase.CORTE_PAPEL,
                StationPhase.IMPRESION
        )) {
            rows.add(buildBaseRow(order, phase, events, stored, intervalRepository, intervalService));
        }

        for (PostpressRecord record : order.getPostpressRecords()) {
            if (record.getType() == PostpressType.FINISHED_PRODUCT) {
                for (PostpressLine line : record.getLines()) {
                    String processKey = "terminado:" + line.getCatalogItemId();
                    rows.add(buildCatalogRow(order, StationPhase.TERMINADOS, processKey, line, events, stored,
                            intervalRepository, intervalService));
                }
            } else if (record.getType() == PostpressType.FINISHING_PROCESS) {
                for (PostpressLine line : record.getLines()) {
                    String processKey = "acabado:" + line.getCatalogItemId();
                    rows.add(buildCatalogRow(order, StationPhase.ACABADOS, processKey, line, events, stored,
                            intervalRepository, intervalService));
                }
            }
        }

        if (order.getPostpressRecords().stream().noneMatch(r -> r.getType() == PostpressType.FINISHED_PRODUCT)) {
            rows.add(buildBaseRow(order, StationPhase.TERMINADOS, events, stored, intervalRepository, intervalService));
        }
        if (order.getPostpressRecords().stream().noneMatch(r -> r.getType() == PostpressType.FINISHING_PROCESS)) {
            rows.add(buildBaseRow(order, StationPhase.ACABADOS, events, stored, intervalRepository, intervalService));
        }

        return rows;
    }

    private static StationProcessRow buildBaseRow(
            ProductionOrder order,
            StationPhase phase,
            List<StationOperationEvent> events,
            List<StationProcessProgress> stored,
            StationOperationIntervalRepositoryPort intervalRepository,
            StationIntervalService intervalService
    ) {
        String processKey = phase.getApiValue();
        int total = order.getRequestedQuantity();
        int completed = StationValidationService.sumUnits(events, StationEventType.AVANCE_UNIDADES, processKey);
        int delivered = StationValidationService.sumDeliveryUnits(events, processKey);
        StationProcessStatus status = stored.stream()
                .filter(p -> processKey.equals(p.getProcessKey()))
                .map(StationProcessProgress::getStatus)
                .findFirst()
                .orElse(resolveStatus(completed, delivered, total));
        List<StationOperationInterval> intervals =
                intervalRepository.findByProductionOrderIdAndProcessKey(
                        order.getCompanyId(), order.getProductionOrderId(), processKey);
        String operatorId = order.getOperators().stream()
                .filter(op -> phase.getOrderStage().map(stage -> stage == op.getStage()).orElse(false))
                .map(op -> op.getUserId())
                .findFirst()
                .orElse(null);
        return new StationProcessRow(
                phase.getApiValue(),
                processKey,
                null,
                null,
                operatorId,
                total,
                completed,
                delivered,
                status.getDbValue(),
                intervalService.sumLaborMs(intervals),
                intervalService.sumPausedMs(intervals)
        );
    }

    private static StationProcessRow buildCatalogRow(
            ProductionOrder order,
            StationPhase phase,
            String processKey,
            PostpressLine line,
            List<StationOperationEvent> events,
            List<StationProcessProgress> stored,
            StationOperationIntervalRepositoryPort intervalRepository,
            StationIntervalService intervalService
    ) {
        // Misma regla que StationProcessKeyResolver: cupo por ítem = cantidad solicitada de la OP.
        int total = order.getRequestedQuantity();
        int completed = StationValidationService.sumUnits(
                events, StationEventType.AVANCE_UNIDADES, processKey, line.getCatalogItemId());
        int delivered = StationValidationService.sumDeliveryUnits(events, processKey, line.getCatalogItemId());
        StationProcessStatus status = stored.stream()
                .filter(p -> processKey.equals(p.getProcessKey()))
                .map(StationProcessProgress::getStatus)
                .findFirst()
                .orElse(resolveStatus(completed, delivered, total));
        List<StationOperationInterval> intervals =
                intervalRepository.findByProductionOrderIdAndProcessKey(
                        order.getCompanyId(), order.getProductionOrderId(), processKey);
        ProductionOrderStage stage = phase == StationPhase.TERMINADOS
                ? ProductionOrderStage.FINISHED_PRODUCTS
                : ProductionOrderStage.FINISHING_PROCESSES;
        String operatorId = order.getOperators().stream()
                .filter(op -> op.getStage() == stage)
                .map(op -> op.getUserId())
                .findFirst()
                .orElse(null);
        return new StationProcessRow(
                phase.getApiValue(),
                processKey,
                line.getCatalogItemId(),
                line.getItemName(),
                operatorId,
                total,
                completed,
                delivered,
                status.getDbValue(),
                intervalService.sumLaborMs(intervals),
                intervalService.sumPausedMs(intervals)
        );
    }

    private static StationProcessStatus resolveStatus(int completed, int delivered, int total) {
        if (total > 0 && delivered >= total) {
            return StationProcessStatus.TERMINADO;
        }
        if (completed > 0) {
            return StationProcessStatus.EN_PROCESO;
        }
        return StationProcessStatus.PENDIENTE;
    }

    public record StationProcessRow(
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
    }
}
