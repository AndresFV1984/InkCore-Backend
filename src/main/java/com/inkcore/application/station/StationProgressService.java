package com.inkcore.application.station;

import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationProcessProgress;
import com.inkcore.domain.station.model.StationProcessStatus;
import com.inkcore.domain.station.ports.out.StationProcessProgressRepositoryPort;
import com.inkcore.domain.station.service.StationProcessKeyResolver;
import com.inkcore.domain.station.service.StationValidationService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StationProgressService {

    private final StationProcessProgressRepositoryPort progressRepository;

    public StationProgressService(StationProcessProgressRepositoryPort progressRepository) {
        this.progressRepository = progressRepository;
    }

    public void updateFromEvent(
            StationOperationEvent event,
            StationProcessKeyResolver.ResolvedProcessKey resolved,
            List<StationOperationEvent> orderEvents,
            LocalDateTime now
    ) {
        if (event.isShiftEvent() || event.getProductionOrderId() == null) {
            return;
        }

        StationProcessProgress progress = progressRepository
                .findByOrderAndProcessKey(event.getProductionOrderId(), resolved.processKey())
                .orElseGet(StationProcessProgress::new);

        if (progress.getProductionOrderId() == null) {
            progress.setCompanyId(event.getCompanyId());
            progress.setProductionOrderId(event.getProductionOrderId());
            progress.setProcessKey(resolved.processKey());
            progress.setUserId(event.getUserId());
            progress.setPhase(resolved.phase());
            progress.setCatalogItemId(resolved.catalogItemId());
            progress.setCompletedUnits(0);
            progress.setDeliveredUnits(0);
            progress.setStatus(StationProcessStatus.PENDIENTE);
            progress.setCreatedAt(now);
        }

        int completed = StationValidationService.sumUnits(
                orderEvents, StationEventType.AVANCE_UNIDADES, resolved.processKey(), resolved.catalogItemId());
        int delivered = StationValidationService.sumDeliveryUnits(
                orderEvents, resolved.processKey(), resolved.catalogItemId());
        progress.setTotalUnits(resolved.totalUnits());
        progress.setCompletedUnits(completed);
        progress.setDeliveredUnits(delivered);
        progress.setLastEventAt(event.getOccurredAt());
        progress.setUpdatedAt(now);
        progress.setStatus(resolveStatus(progress, event.getEventType()));
        progressRepository.save(progress);
    }

    private StationProcessStatus resolveStatus(StationProcessProgress progress, StationEventType eventType) {
        if (eventType == StationEventType.ENTREGA_TOTAL
                || (progress.getTotalUnits() > 0 && progress.getDeliveredUnits() >= progress.getTotalUnits())) {
            return StationProcessStatus.TERMINADO;
        }
        if (eventType == StationEventType.INICIO_FASE
                || eventType == StationEventType.AVANCE_UNIDADES
                || eventType == StationEventType.REANUDACION
                || progress.getCompletedUnits() > 0) {
            return StationProcessStatus.EN_PROCESO;
        }
        return StationProcessStatus.PENDIENTE;
    }
}
