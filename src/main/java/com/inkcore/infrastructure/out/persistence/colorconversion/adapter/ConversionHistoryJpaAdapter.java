package com.inkcore.infrastructure.out.persistence.colorconversion.adapter;

import com.inkcore.domain.colorconversion.model.ConversionHistory;
import com.inkcore.domain.colorconversion.ports.out.ConversionHistoryRepositoryPort;
import com.inkcore.infrastructure.out.persistence.colorconversion.entity.ConversionHistoryEntity;
import com.inkcore.infrastructure.out.persistence.colorconversion.mapper.ConversionHistoryPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.colorconversion.repository.JpaConversionHistoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConversionHistoryJpaAdapter implements ConversionHistoryRepositoryPort {

    private final JpaConversionHistoryRepository jpaConversionHistoryRepository;
    private final ConversionHistoryPersistenceMapper mapper;

    public ConversionHistoryJpaAdapter(
            JpaConversionHistoryRepository jpaConversionHistoryRepository,
            ConversionHistoryPersistenceMapper mapper
    ) {
        this.jpaConversionHistoryRepository = jpaConversionHistoryRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ConversionHistory save(ConversionHistory history) {
        ConversionHistoryEntity existing = jpaConversionHistoryRepository
                .findById(history.getConversionHistoryId())
                .orElse(null);
        if (existing == null) {
            ConversionHistoryEntity saved = jpaConversionHistoryRepository.save(mapper.toNewEntity(history));
            return mapper.toDomain(saved);
        }
        mapper.copyScalars(history, existing);
        return mapper.toDomain(jpaConversionHistoryRepository.save(existing));
    }
}
