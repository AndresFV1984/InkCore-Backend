package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationIntervalService;
import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.station.model.StationIntervalKind;
import com.inkcore.domain.station.model.StationOperationInterval;
import com.inkcore.domain.station.ports.out.StationOperationIntervalRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class GetStationLaborSettlementUseCase {

    private final StationSupport support;
    private final StationOperationIntervalRepositoryPort intervalRepository;
    private final StationIntervalService intervalService;

    public GetStationLaborSettlementUseCase(
            StationSupport support,
            StationOperationIntervalRepositoryPort intervalRepository,
            StationIntervalService intervalService
    ) {
        this.support = support;
        this.intervalRepository = intervalRepository;
        this.intervalService = intervalService;
    }

    @Transactional(readOnly = true)
    public LaborSettlement execute(
            String userId,
            LocalDate from,
            LocalDate to,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String targetUserId = userId == null || userId.isBlank()
                ? support.userId(authentication)
                : userId.trim();

        LocalDateTime fromDt = from == null
                ? LocalDate.now().minusDays(7).atStartOfDay()
                : from.atStartOfDay();
        LocalDateTime toDt = to == null
                ? LocalDate.now().atTime(LocalTime.MAX)
                : to.atTime(LocalTime.MAX);

        List<StationOperationInterval> intervals = intervalRepository
                .findByCompanyIdAndUserIdAndDateRange(companyId, targetUserId, fromDt, toDt);

        long grossShiftMs = intervals.stream()
                .filter(i -> i.getIntervalKind() == StationIntervalKind.SHIFT)
                .map(StationOperationInterval::getDurationMs)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        long pausedMs = intervalService.sumPausedMs(intervals);
        long netLaborMs = intervalService.sumLaborMs(intervals);
        boolean shiftOpen = intervalRepository.findOpenShiftInterval(companyId, targetUserId).isPresent();

        List<LaborInterval> mapped = intervals.stream()
                .map(i -> new LaborInterval(
                        i.getIntervalId(),
                        i.getIntervalKind().getDbValue(),
                        i.getStartedAt() == null ? null : i.getStartedAt().toString(),
                        i.getEndedAt() == null ? null : i.getEndedAt().toString(),
                        i.getDurationMs(),
                        i.getProductionOrderId(),
                        i.getProcessKey()
                ))
                .toList();

        return new LaborSettlement(
                targetUserId,
                fromDt.toString(),
                toDt.toString(),
                shiftOpen,
                grossShiftMs,
                pausedMs,
                netLaborMs,
                mapped
        );
    }

    public record LaborSettlement(
            String userId,
            String from,
            String to,
            boolean shiftOpen,
            long grossShiftMs,
            long pausedMs,
            long netLaborMs,
            List<LaborInterval> intervals
    ) {
    }

    public record LaborInterval(
            String id,
            String kind,
            String startedAt,
            String endedAt,
            Long durationMs,
            String orderId,
            String processKey
    ) {
    }
}
