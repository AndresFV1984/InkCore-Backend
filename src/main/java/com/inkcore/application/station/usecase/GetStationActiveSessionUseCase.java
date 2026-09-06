package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.station.model.StationIntervalKind;
import com.inkcore.domain.station.model.StationOperationInterval;
import com.inkcore.domain.station.ports.out.StationOperationIntervalRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetStationActiveSessionUseCase {

    private final StationSupport support;
    private final StationOperationIntervalRepositoryPort intervalRepository;

    public GetStationActiveSessionUseCase(
            StationSupport support,
            StationOperationIntervalRepositoryPort intervalRepository
    ) {
        this.support = support;
        this.intervalRepository = intervalRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ActiveSession> execute(Authentication authentication) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);

        Optional<StationOperationInterval> labor = intervalRepository
                .findOpenIntervalsByUser(companyId, userId, StationIntervalKind.LABOR)
                .stream()
                .findFirst();
        if (labor.isPresent()) {
            return Optional.of(ActiveSession.from(labor.get()));
        }

        Optional<StationOperationInterval> pause = intervalRepository
                .findOpenIntervalsByUser(companyId, userId, StationIntervalKind.PAUSE)
                .stream()
                .findFirst();
        return pause.map(ActiveSession::from);
    }

    public record ActiveSession(
            String intervalId,
            String productionOrderId,
            String processKey,
            String phase,
            String intervalKind,
            String startedAt,
            boolean open
    ) {
        static ActiveSession from(StationOperationInterval interval) {
            return new ActiveSession(
                    interval.getIntervalId(),
                    interval.getProductionOrderId(),
                    interval.getProcessKey(),
                    interval.getPhase(),
                    interval.getIntervalKind().getDbValue(),
                    interval.getStartedAt() == null ? null : interval.getStartedAt().toString(),
                    interval.isOpen()
            );
        }
    }
}
