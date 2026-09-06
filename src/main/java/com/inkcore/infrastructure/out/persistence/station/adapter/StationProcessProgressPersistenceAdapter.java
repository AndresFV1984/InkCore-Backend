package com.inkcore.infrastructure.out.persistence.station.adapter;

import com.inkcore.domain.station.model.StationProcessProgress;
import com.inkcore.domain.station.ports.out.StationProcessProgressRepositoryPort;
import com.inkcore.infrastructure.out.persistence.station.entity.StationProcessProgressEntity;
import com.inkcore.infrastructure.out.persistence.station.mapper.StationPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.station.repository.JpaStationProcessProgressRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class StationProcessProgressPersistenceAdapter implements StationProcessProgressRepositoryPort {

    private final JpaStationProcessProgressRepository repository;

    public StationProcessProgressPersistenceAdapter(JpaStationProcessProgressRepository repository) {
        this.repository = repository;
    }

    @Override
    public StationProcessProgress save(StationProcessProgress progress) {
        var id = new StationProcessProgressEntity.IdKey(
                progress.getProductionOrderId(),
                progress.getProcessKey()
        );
        var existing = repository.findById(id).orElse(null);
        if (existing == null) {
            return StationPersistenceMapper.toDomain(
                    repository.save(StationPersistenceMapper.toEntity(progress))
            );
        }
        StationPersistenceMapper.copyProgress(progress, existing);
        return StationPersistenceMapper.toDomain(repository.save(existing));
    }

    @Override
    public Optional<StationProcessProgress> findByOrderAndProcessKey(
            String productionOrderId,
            String processKey
    ) {
        return repository.findById(new StationProcessProgressEntity.IdKey(productionOrderId, processKey))
                .map(StationPersistenceMapper::toDomain);
    }

    @Override
    public List<StationProcessProgress> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository.findAllByCompanyIdAndProductionOrderId(companyId, productionOrderId)
                .stream()
                .map(StationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<StationProcessProgress> findByCompanyIdAndUserId(String companyId, String userId) {
        return repository.findAllByCompanyIdAndUserId(companyId, userId)
                .stream()
                .map(StationPersistenceMapper::toDomain)
                .toList();
    }
}
