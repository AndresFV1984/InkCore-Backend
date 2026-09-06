package com.inkcore.infrastructure.out.persistence.station.adapter;

import com.inkcore.domain.station.model.StationIntervalKind;
import com.inkcore.domain.station.model.StationOperationInterval;
import com.inkcore.domain.station.ports.out.StationOperationIntervalRepositoryPort;
import com.inkcore.infrastructure.out.persistence.station.mapper.StationPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.station.repository.JpaStationOperationIntervalRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class StationOperationIntervalPersistenceAdapter implements StationOperationIntervalRepositoryPort {

    private final JpaStationOperationIntervalRepository repository;

    public StationOperationIntervalPersistenceAdapter(JpaStationOperationIntervalRepository repository) {
        this.repository = repository;
    }

    @Override
    public StationOperationInterval save(StationOperationInterval interval) {
        var existing = repository.findById(interval.getIntervalId()).orElse(null);
        if (existing == null) {
            return StationPersistenceMapper.toDomain(
                    repository.save(StationPersistenceMapper.toEntity(interval))
            );
        }
        StationPersistenceMapper.copyInterval(interval, existing);
        return StationPersistenceMapper.toDomain(repository.save(existing));
    }

    @Override
    public Optional<StationOperationInterval> findOpenInterval(
            String companyId,
            String userId,
            String productionOrderId,
            String processKey,
            StationIntervalKind kind
    ) {
        return repository.findFirstByCompanyIdAndUserIdAndProductionOrderIdAndProcessKeyAndIntervalKindAndOpenTrue(
                        companyId, userId, productionOrderId, processKey, kind.getDbValue())
                .map(StationPersistenceMapper::toDomain);
    }

    @Override
    public List<StationOperationInterval> findOpenIntervals(
            String companyId,
            String userId,
            String productionOrderId,
            String processKey,
            StationIntervalKind kind
    ) {
        return repository.findAllByCompanyIdAndUserIdAndProductionOrderIdAndProcessKeyAndIntervalKindAndOpenTrue(
                        companyId, userId, productionOrderId, processKey, kind.getDbValue())
                .stream()
                .map(StationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<StationOperationInterval> findByCompanyIdAndUserIdAndDateRange(
            String companyId,
            String userId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return repository.findAllByCompanyIdAndUserIdAndStartedAtBetweenOrderByStartedAtAsc(
                        companyId, userId, from, to)
                .stream()
                .map(StationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<StationOperationInterval> findByProductionOrderIdAndProcessKey(
            String companyId,
            String productionOrderId,
            String processKey
    ) {
        return repository.findAllByCompanyIdAndProductionOrderIdAndProcessKeyOrderByStartedAtAsc(
                        companyId, productionOrderId, processKey)
                .stream()
                .map(StationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<StationOperationInterval> findOpenIntervalsByUser(
            String companyId,
            String userId,
            StationIntervalKind kind
    ) {
        return repository.findFirstByCompanyIdAndUserIdAndIntervalKindAndOpenTrue(
                        companyId, userId, kind.getDbValue())
                .stream()
                .map(StationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<StationOperationInterval> findOpenShiftInterval(String companyId, String userId) {
        return repository.findFirstByCompanyIdAndUserIdAndIntervalKindAndOpenTrue(
                        companyId, userId, StationIntervalKind.SHIFT.getDbValue())
                .map(StationPersistenceMapper::toDomain);
    }
}
