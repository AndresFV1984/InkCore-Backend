package com.inkcore.application.station;

import com.inkcore.domain.station.exception.StationBusinessRuleException;
import com.inkcore.domain.station.model.StationIntervalKind;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationOperationInterval;
import com.inkcore.domain.station.model.StationPauseReason;
import com.inkcore.domain.station.ports.out.StationOperationIntervalRepositoryPort;
import com.inkcore.domain.station.service.StationProcessKeyResolver;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class StationIntervalService {

    private final StationOperationIntervalRepositoryPort intervalRepository;

    public StationIntervalService(StationOperationIntervalRepositoryPort intervalRepository) {
        this.intervalRepository = intervalRepository;
    }

    public void applyEvent(
            StationOperationEvent event,
            StationProcessKeyResolver.ResolvedProcessKey resolved
    ) {
        switch (event.getEventType()) {
            case INICIO_FASE -> {
                // Evita intervalos labor duplicados si el SPA reintenta phase/start.
                closeOpenInterval(event, StationIntervalKind.LABOR);
                openInterval(event, resolved, StationIntervalKind.LABOR);
            }
            case FIN_FASE -> closeOpenInterval(event, StationIntervalKind.LABOR);
            case PARO -> {
                closeOpenInterval(event, StationIntervalKind.LABOR);
                openInterval(event, resolved, StationIntervalKind.PAUSE);
            }
            case REANUDACION -> {
                closeOpenInterval(event, StationIntervalKind.PAUSE);
                openInterval(event, resolved, StationIntervalKind.LABOR);
            }
            case MARCA_HORARIO -> handleShiftMark(event, resolved);
            default -> {
            }
        }
    }

    private void handleShiftMark(
            StationOperationEvent event,
            StationProcessKeyResolver.ResolvedProcessKey resolved
    ) {
        StationPauseReason reason = StationPauseReason.fromValue(event.getPauseReason());
        if (reason == StationPauseReason.INICIO_HORARIO) {
            openInterval(event, resolved, StationIntervalKind.SHIFT);
        } else if (reason == StationPauseReason.FIN_HORARIO) {
            closeOpenShiftInterval(event);
        }
    }

    private void openInterval(
            StationOperationEvent event,
            StationProcessKeyResolver.ResolvedProcessKey resolved,
            StationIntervalKind kind
    ) {
        StationOperationInterval interval = new StationOperationInterval();
        interval.setCompanyId(event.getCompanyId());
        interval.setProductionOrderId(event.getProductionOrderId());
        interval.setClientId(event.getClientId());
        interval.setUserId(event.getUserId());
        interval.setProcessKey(resolved.processKey());
        interval.setPhase(resolved.phase());
        interval.setCatalogItemId(resolved.catalogItemId());
        interval.setIntervalKind(kind);
        interval.setStartedAt(event.getOccurredAt());
        interval.setPauseReason(event.getPauseReason());
        interval.setNote(event.getNote());
        interval.setOpenedByEventId(event.getEventId());
        interval.setOpen(true);
        interval.setCreatedAt(event.getCreatedAt());
        intervalRepository.save(interval);
    }

    private void closeOpenInterval(StationOperationEvent event, StationIntervalKind kind) {
        List<StationOperationInterval> openIntervals = intervalRepository.findOpenIntervals(
                event.getCompanyId(),
                event.getUserId(),
                event.getProductionOrderId(),
                event.getProcessKey(),
                kind
        );
        for (StationOperationInterval open : openIntervals) {
            closeInterval(open, event);
        }
    }

    private void closeOpenShiftInterval(StationOperationEvent event) {
        intervalRepository.findOpenShiftInterval(event.getCompanyId(), event.getUserId())
                .ifPresent(open -> closeInterval(open, event));
    }

    private void closeInterval(StationOperationInterval open, StationOperationEvent closingEvent) {
        if (open.getStartedAt() != null
                && closingEvent.getOccurredAt() != null
                && closingEvent.getOccurredAt().isBefore(open.getStartedAt())) {
            throw new StationBusinessRuleException(
                    "occurredAt (" + closingEvent.getOccurredAt()
                            + ") no puede ser anterior al inicio del intervalo abierto ("
                            + open.getStartedAt() + ")"
            );
        }
        open.setEndedAt(closingEvent.getOccurredAt());
        open.setClosedByEventId(closingEvent.getEventId());
        open.setOpen(false);
        if (open.getStartedAt() != null && open.getEndedAt() != null) {
            long durationMs = Duration.between(open.getStartedAt(), open.getEndedAt()).toMillis();
            open.setDurationMs(Math.max(0L, durationMs));
        }
        intervalRepository.save(open);
    }

    public long sumLaborMs(List<StationOperationInterval> intervals) {
        return intervals.stream()
                .filter(interval -> interval.getIntervalKind() == StationIntervalKind.LABOR)
                .map(StationOperationInterval::getDurationMs)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
    }

    public long sumPausedMs(List<StationOperationInterval> intervals) {
        return intervals.stream()
                .filter(interval -> interval.getIntervalKind() == StationIntervalKind.PAUSE)
                .map(StationOperationInterval::getDurationMs)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
    }

    public boolean isPausedNow(List<StationOperationEvent> events, String processKey, String userId) {
        return com.inkcore.domain.station.service.StationValidationService.hasOpenPause(events, processKey, userId);
    }
}
