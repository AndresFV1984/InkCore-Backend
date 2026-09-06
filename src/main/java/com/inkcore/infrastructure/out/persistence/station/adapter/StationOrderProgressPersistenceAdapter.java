package com.inkcore.infrastructure.out.persistence.station.adapter;

import com.inkcore.domain.station.model.StationOrderProgress;
import com.inkcore.domain.station.ports.out.StationOrderProgressRepositoryPort;
import com.inkcore.infrastructure.out.persistence.station.mapper.StationPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.station.repository.JpaStationOrderProgressRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class StationOrderProgressPersistenceAdapter implements StationOrderProgressRepositoryPort {

    private final JpaStationOrderProgressRepository repository;

    public StationOrderProgressPersistenceAdapter(JpaStationOrderProgressRepository repository) {
        this.repository = repository;
    }

    @Override
    public StationOrderProgress save(StationOrderProgress progress) {
        var existing = repository.findById(progress.getProductionOrderId()).orElse(null);
        if (existing == null) {
            return StationPersistenceMapper.toDomain(
                    repository.save(StationPersistenceMapper.toEntity(progress))
            );
        }
        StationPersistenceMapper.copyOrderProgress(progress, existing);
        return StationPersistenceMapper.toDomain(repository.save(existing));
    }

    @Override
    public Optional<StationOrderProgress> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository.findByCompanyIdAndProductionOrderId(companyId, productionOrderId)
                .map(StationPersistenceMapper::toDomain);
    }

    @Override
    public List<StationOrderProgress> findByProductionOrderIds(String companyId, Collection<String> productionOrderIds) {
        if (productionOrderIds == null || productionOrderIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllByCompanyIdAndProductionOrderIdIn(companyId, productionOrderIds)
                .stream()
                .map(StationPersistenceMapper::toDomain)
                .toList();
    }
}
