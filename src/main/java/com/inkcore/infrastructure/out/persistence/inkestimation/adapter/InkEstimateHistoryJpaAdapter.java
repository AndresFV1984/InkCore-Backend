package com.inkcore.infrastructure.out.persistence.inkestimation.adapter;

import com.inkcore.domain.inkestimation.model.InkEstimateHistory;
import com.inkcore.domain.inkestimation.ports.out.InkEstimateHistoryRepositoryPort;
import com.inkcore.infrastructure.out.persistence.inkestimation.entity.InkEstimateHistoryEntity;
import com.inkcore.infrastructure.out.persistence.inkestimation.mapper.InkEstimateHistoryPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.inkestimation.repository.JpaInkEstimateHistoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InkEstimateHistoryJpaAdapter implements InkEstimateHistoryRepositoryPort {

    private final JpaInkEstimateHistoryRepository repository;
    private final InkEstimateHistoryPersistenceMapper mapper;

    public InkEstimateHistoryJpaAdapter(
            JpaInkEstimateHistoryRepository repository,
            InkEstimateHistoryPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public InkEstimateHistory save(InkEstimateHistory history) {
        InkEstimateHistoryEntity existing = repository.findById(history.getInkEstimateHistoryId()).orElse(null);
        if (existing == null) {
            return mapper.toDomain(repository.save(mapper.toNewEntity(history)));
        }
        mapper.copyScalars(history, existing);
        return mapper.toDomain(repository.save(existing));
    }
}
