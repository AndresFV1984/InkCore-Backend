package com.inkcore.infrastructure.out.persistence.station.adapter;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.station.model.StationEventFilter;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import com.inkcore.infrastructure.out.persistence.station.entity.StationOperationEventEntity;
import com.inkcore.infrastructure.out.persistence.station.mapper.StationPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.station.repository.JpaStationOperationEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class StationOperationEventPersistenceAdapter implements StationOperationEventRepositoryPort {

    private final JpaStationOperationEventRepository repository;

    public StationOperationEventPersistenceAdapter(JpaStationOperationEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public StationOperationEvent save(StationOperationEvent event) {
        return StationPersistenceMapper.toDomain(
                repository.save(StationPersistenceMapper.toEntity(event))
        );
    }

    @Override
    public Optional<StationOperationEvent> findById(String eventId) {
        return repository.findById(eventId).map(StationPersistenceMapper::toDomain);
    }

    @Override
    public List<StationOperationEvent> findAllByProductionOrderId(String companyId, String productionOrderId) {
        return repository.findAllByCompanyIdAndProductionOrderIdOrderByOccurredAtAsc(companyId, productionOrderId)
                .stream()
                .map(StationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<StationOperationEvent> findAllByProductionOrderIdAndProcessKey(
            String companyId,
            String productionOrderId,
            String processKey
    ) {
        return repository.findAllByCompanyIdAndProductionOrderIdAndProcessKeyOrderByOccurredAtAsc(
                        companyId, productionOrderId, processKey)
                .stream()
                .map(StationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<StationOperationEvent> findPage(StationEventFilter filter, PageQuery pageQuery) {
        Page<StationOperationEventEntity> page = repository.findAll(
                toSpecification(filter),
                PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(Sort.Direction.DESC, "occurredAt"))
        );
        return new PageResult<>(
                page.getContent().stream().map(StationPersistenceMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    private static Specification<StationOperationEventEntity> toSpecification(StationEventFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("companyId"), filter.companyId()));
            if (filter.productionOrderId() != null) {
                predicates.add(builder.equal(root.get("productionOrderId"), filter.productionOrderId()));
            }
            if (filter.userId() != null) {
                predicates.add(builder.equal(root.get("userId"), filter.userId()));
            }
            if (filter.phase() != null) {
                predicates.add(builder.equal(root.get("phase"), filter.phase()));
            }
            if (filter.processKey() != null) {
                predicates.add(builder.equal(root.get("processKey"), filter.processKey()));
            }
            if (filter.catalogItemId() != null) {
                predicates.add(builder.equal(root.get("catalogItemId"), filter.catalogItemId()));
            }
            if (filter.eventType() != null) {
                predicates.add(builder.equal(root.get("eventType"), filter.eventType().getDbValue()));
            }
            if (filter.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), filter.to()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Override
    public List<StationOperationEvent> findByCompanyIdAndUserIdAndDateRange(
            String companyId,
            String userId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return repository.findAllByCompanyIdAndUserIdAndOccurredAtBetweenOrderByOccurredAtAsc(
                        companyId, userId, from, to)
                .stream()
                .map(StationPersistenceMapper::toDomain)
                .toList();
    }
}
