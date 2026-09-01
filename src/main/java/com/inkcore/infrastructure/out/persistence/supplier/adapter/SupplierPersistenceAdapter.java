package com.inkcore.infrastructure.out.persistence.supplier.adapter;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.supplier.model.Supplier;
import com.inkcore.domain.supplier.ports.out.SupplierRepositoryPort;
import com.inkcore.infrastructure.out.persistence.supplier.entity.SupplierEntity;
import com.inkcore.infrastructure.out.persistence.supplier.mapper.SupplierPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.supplier.repository.JpaSupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class SupplierPersistenceAdapter implements SupplierRepositoryPort {

    private final JpaSupplierRepository jpaSupplierRepository;
    private final SupplierPersistenceMapper mapper;

    public SupplierPersistenceAdapter(
            JpaSupplierRepository jpaSupplierRepository,
            SupplierPersistenceMapper mapper
    ) {
        this.jpaSupplierRepository = jpaSupplierRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Supplier save(Supplier supplier) {
        SupplierEntity existing = jpaSupplierRepository.findById(supplier.getSupplierId()).orElse(null);
        if (existing == null) {
            SupplierEntity entity = mapper.toNewEntity(supplier);
            SupplierEntity saved = jpaSupplierRepository.save(entity);
            return mapper.toDomain(saved);
        }
        mapper.copyScalars(supplier, existing);
        SupplierEntity saved = jpaSupplierRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Supplier> findById(String supplierId) {
        return jpaSupplierRepository.findById(supplierId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Supplier> findPage(PageQuery pageQuery) {
        return mapPage(jpaSupplierRepository.findAll(pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Supplier> findPageByState(boolean state, PageQuery pageQuery) {
        return mapPage(jpaSupplierRepository.findAllByState(state, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Supplier> findPageByCompanyId(String companyId, PageQuery pageQuery) {
        return mapPage(jpaSupplierRepository.findAllByCompanyId(companyId, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Supplier> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery) {
        return mapPage(
                jpaSupplierRepository.findAllByCompanyIdAndState(companyId, state, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndIdentificationIgnoreCase(String companyId, String identification) {
        return companyId != null
                && identification != null
                && jpaSupplierRepository.existsByCompanyIdAndIdentificationIgnoreCase(
                companyId.trim(), identification.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndIdentificationIgnoreCaseExcludingSupplierId(
            String companyId,
            String identification,
            String supplierId
    ) {
        return companyId != null
                && identification != null
                && supplierId != null
                && jpaSupplierRepository.existsByCompanyIdAndIdentificationIgnoreCaseAndSupplierIdNot(
                companyId.trim(), identification.trim(), supplierId);
    }

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(Sort.Direction.ASC, "name"));
    }

    private PageResult<Supplier> mapPage(Page<SupplierEntity> page, PageQuery pageQuery) {
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }
}
