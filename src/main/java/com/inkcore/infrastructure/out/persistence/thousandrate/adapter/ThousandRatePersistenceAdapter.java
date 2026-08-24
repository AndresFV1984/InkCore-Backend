package com.inkcore.infrastructure.out.persistence.thousandrate.adapter;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.thousandrate.model.ThousandRate;
import com.inkcore.domain.thousandrate.ports.out.ThousandRateRepositoryPort;
import com.inkcore.infrastructure.out.persistence.thousandrate.entity.ThousandRateEntity;
import com.inkcore.infrastructure.out.persistence.thousandrate.mapper.ThousandRatePersistenceMapper;
import com.inkcore.infrastructure.out.persistence.thousandrate.repository.JpaThousandRateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class ThousandRatePersistenceAdapter implements ThousandRateRepositoryPort {

    private final JpaThousandRateRepository jpaThousandRateRepository;
    private final ThousandRatePersistenceMapper mapper;

    public ThousandRatePersistenceAdapter(
            JpaThousandRateRepository jpaThousandRateRepository,
            ThousandRatePersistenceMapper mapper
    ) {
        this.jpaThousandRateRepository = jpaThousandRateRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ThousandRate save(ThousandRate thousandRate) {
        ThousandRateEntity existing = jpaThousandRateRepository
                .findById(thousandRate.getThousandRateId())
                .orElse(null);
        if (existing == null) {
            ThousandRateEntity entity = mapper.toNewEntity(thousandRate);
            ThousandRateEntity saved = jpaThousandRateRepository.save(entity);
            return mapper.toDomain(saved);
        }
        mapper.copyScalars(thousandRate, existing);
        ThousandRateEntity saved = jpaThousandRateRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ThousandRate> findById(String thousandRateId) {
        return jpaThousandRateRepository.findById(thousandRateId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ThousandRate> findPage(PageQuery pageQuery) {
        return mapPage(jpaThousandRateRepository.findAll(pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ThousandRate> findPageByState(boolean state, PageQuery pageQuery) {
        return mapPage(jpaThousandRateRepository.findAllByState(state, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ThousandRate> findPageByCompanyId(String companyId, PageQuery pageQuery) {
        return mapPage(
                jpaThousandRateRepository.findAllByCompanyId(companyId, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ThousandRate> findPageByCompanyIdAndState(
            String companyId,
            boolean state,
            PageQuery pageQuery
    ) {
        return mapPage(
                jpaThousandRateRepository.findAllByCompanyIdAndState(companyId, state, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name) {
        return companyId != null
                && name != null
                && jpaThousandRateRepository.existsByCompanyIdAndNameIgnoreCase(
                companyId.trim(), name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCaseExcludingThousandRateId(
            String companyId,
            String name,
            String thousandRateId
    ) {
        return companyId != null
                && name != null
                && thousandRateId != null
                && jpaThousandRateRepository.existsByCompanyIdAndNameIgnoreCaseAndThousandRateIdNot(
                companyId.trim(), name.trim(), thousandRateId);
    }

    @Override
    @Transactional
    public void clearDefaultForCompanyAndColorCategoryExcept(
            String companyId,
            String colorCategory,
            String thousandRateId
    ) {
        if (companyId == null || colorCategory == null || thousandRateId == null) {
            return;
        }
        jpaThousandRateRepository.clearDefaultForCompanyAndColorCategoryExcept(
                companyId.trim(),
                colorCategory.trim(),
                thousandRateId
        );
    }

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Order.asc("name"))
        );
    }

    private PageResult<ThousandRate> mapPage(Page<ThousandRateEntity> page, PageQuery pageQuery) {
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }
}
