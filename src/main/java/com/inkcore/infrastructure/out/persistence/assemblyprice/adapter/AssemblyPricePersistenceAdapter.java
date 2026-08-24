package com.inkcore.infrastructure.out.persistence.assemblyprice.adapter;

import com.inkcore.domain.assemblyprice.model.AssemblyPrice;
import com.inkcore.domain.assemblyprice.ports.out.AssemblyPriceRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.assemblyprice.entity.AssemblyPriceEntity;
import com.inkcore.infrastructure.out.persistence.assemblyprice.mapper.AssemblyPricePersistenceMapper;
import com.inkcore.infrastructure.out.persistence.assemblyprice.repository.JpaAssemblyPriceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class AssemblyPricePersistenceAdapter implements AssemblyPriceRepositoryPort {

    private final JpaAssemblyPriceRepository jpaAssemblyPriceRepository;
    private final AssemblyPricePersistenceMapper mapper;

    public AssemblyPricePersistenceAdapter(
            JpaAssemblyPriceRepository jpaAssemblyPriceRepository,
            AssemblyPricePersistenceMapper mapper
    ) {
        this.jpaAssemblyPriceRepository = jpaAssemblyPriceRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public AssemblyPrice save(AssemblyPrice assemblyPrice) {
        AssemblyPriceEntity existing = jpaAssemblyPriceRepository
                .findById(assemblyPrice.getAssemblyPriceId())
                .orElse(null);
        if (existing == null) {
            AssemblyPriceEntity entity = mapper.toNewEntity(assemblyPrice);
            AssemblyPriceEntity saved = jpaAssemblyPriceRepository.save(entity);
            return mapper.toDomain(saved);
        }
        mapper.copyScalars(assemblyPrice, existing);
        AssemblyPriceEntity saved = jpaAssemblyPriceRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssemblyPrice> findById(String assemblyPriceId) {
        return jpaAssemblyPriceRepository.findById(assemblyPriceId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssemblyPrice> findPage(PageQuery pageQuery) {
        return mapPage(jpaAssemblyPriceRepository.findAll(pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssemblyPrice> findPageByState(boolean state, PageQuery pageQuery) {
        return mapPage(jpaAssemblyPriceRepository.findAllByState(state, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssemblyPrice> findPageByCompanyId(String companyId, PageQuery pageQuery) {
        return mapPage(
                jpaAssemblyPriceRepository.findAllByCompanyId(companyId, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssemblyPrice> findPageByCompanyIdAndState(
            String companyId,
            boolean state,
            PageQuery pageQuery
    ) {
        return mapPage(
                jpaAssemblyPriceRepository.findAllByCompanyIdAndState(companyId, state, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name) {
        return companyId != null
                && name != null
                && jpaAssemblyPriceRepository.existsByCompanyIdAndNameIgnoreCase(
                companyId.trim(), name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCaseExcludingAssemblyPriceId(
            String companyId,
            String name,
            String assemblyPriceId
    ) {
        return companyId != null
                && name != null
                && assemblyPriceId != null
                && jpaAssemblyPriceRepository.existsByCompanyIdAndNameIgnoreCaseAndAssemblyPriceIdNot(
                companyId.trim(), name.trim(), assemblyPriceId);
    }

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Order.asc("name"))
        );
    }

    private PageResult<AssemblyPrice> mapPage(Page<AssemblyPriceEntity> page, PageQuery pageQuery) {
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }
}
