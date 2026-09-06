package com.inkcore.infrastructure.out.persistence.order.adapter;

import com.inkcore.domain.order.model.ArSummary;
import com.inkcore.domain.order.ports.out.ArSummaryRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.order.entity.ArSummaryEntity;
import com.inkcore.infrastructure.out.persistence.order.mapper.OrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.order.repository.JpaArSummaryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ArSummaryPersistenceAdapter implements ArSummaryRepositoryPort {

    private final JpaArSummaryRepository repository;

    public ArSummaryPersistenceAdapter(JpaArSummaryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ArSummary> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository.findByCompanyIdAndProductionOrderId(companyId, productionOrderId)
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<ArSummary> findPage(
            String companyId,
            String status,
            String clientId,
            PageQuery pageQuery
    ) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        Page<ArSummaryEntity> page = repository.findAll(
                toSpecification(companyId, status, clientId),
                PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        return new PageResult<>(
                page.getContent().stream().map(OrderPersistenceMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    private static Specification<ArSummaryEntity> toSpecification(
            String companyId,
            String status,
            String clientId
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("companyId"), companyId));
            if (status != null && !status.isBlank()) {
                predicates.add(builder.equal(root.get("status"), status.trim().toLowerCase()));
            }
            if (clientId != null && !clientId.isBlank()) {
                predicates.add(builder.equal(root.get("clientId"), clientId.trim()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
