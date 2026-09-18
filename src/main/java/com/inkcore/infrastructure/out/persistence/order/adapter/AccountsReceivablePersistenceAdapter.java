package com.inkcore.infrastructure.out.persistence.order.adapter;

import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.order.entity.AccountsReceivableEntity;
import com.inkcore.infrastructure.out.persistence.order.mapper.OrderPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.order.repository.JpaAccountsReceivableRepository;
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
public class AccountsReceivablePersistenceAdapter implements AccountsReceivableRepositoryPort {

    private final JpaAccountsReceivableRepository repository;

    public AccountsReceivablePersistenceAdapter(JpaAccountsReceivableRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AccountsReceivable> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository.findByCompanyIdAndProductionOrderId(companyId, productionOrderId)
                .map(OrderPersistenceMapper::toDomain);
    }

    @Override
    public List<AccountsReceivable> findByClientId(String companyId, String clientId) {
        return repository.findAllByCompanyIdAndClientIdOrderByUpdatedAtDesc(companyId, clientId)
                .stream()
                .map(OrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<AccountsReceivable> findPage(
            String companyId,
            String status,
            String clientId,
            PageQuery pageQuery
    ) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        Page<AccountsReceivableEntity> page = repository.findAll(
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

    private static Specification<AccountsReceivableEntity> toSpecification(
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
