package com.inkcore.infrastructure.out.persistence.platetype.adapter;

import com.inkcore.domain.platetype.model.PlateType;
import com.inkcore.domain.platetype.ports.out.PlateTypeRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.platetype.entity.PlateTypeEntity;
import com.inkcore.infrastructure.out.persistence.platetype.mapper.PlateTypePersistenceMapper;
import com.inkcore.infrastructure.out.persistence.platetype.repository.JpaPlateTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class PlateTypePersistenceAdapter implements PlateTypeRepositoryPort {

    private final JpaPlateTypeRepository jpaPlateTypeRepository;
    private final PlateTypePersistenceMapper mapper;

    public PlateTypePersistenceAdapter(
            JpaPlateTypeRepository jpaPlateTypeRepository,
            PlateTypePersistenceMapper mapper
    ) {
        this.jpaPlateTypeRepository = jpaPlateTypeRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PlateType save(PlateType plateType) {
        PlateTypeEntity existing = jpaPlateTypeRepository
                .findById(plateType.getPlateTypeId())
                .orElse(null);
        if (existing == null) {
            PlateTypeEntity entity = mapper.toNewEntity(plateType);
            PlateTypeEntity saved = jpaPlateTypeRepository.save(entity);
            return mapper.toDomain(saved);
        }
        mapper.copyScalars(plateType, existing);
        PlateTypeEntity saved = jpaPlateTypeRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlateType> findById(String plateTypeId) {
        return jpaPlateTypeRepository.findById(plateTypeId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PlateType> findPage(PageQuery pageQuery) {
        return mapPage(jpaPlateTypeRepository.findAll(pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PlateType> findPageByState(boolean state, PageQuery pageQuery) {
        return mapPage(jpaPlateTypeRepository.findAllByState(state, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PlateType> findPageByCompanyId(String companyId, PageQuery pageQuery) {
        return mapPage(
                jpaPlateTypeRepository.findAllByCompanyId(companyId, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PlateType> findPageByCompanyIdAndState(
            String companyId,
            boolean state,
            PageQuery pageQuery
    ) {
        return mapPage(
                jpaPlateTypeRepository.findAllByCompanyIdAndState(companyId, state, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name) {
        return companyId != null
                && name != null
                && jpaPlateTypeRepository.existsByCompanyIdAndNameIgnoreCase(
                companyId.trim(), name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCaseExcludingPlateTypeId(
            String companyId,
            String name,
            String plateTypeId
    ) {
        return companyId != null
                && name != null
                && plateTypeId != null
                && jpaPlateTypeRepository.existsByCompanyIdAndNameIgnoreCaseAndPlateTypeIdNot(
                companyId.trim(), name.trim(), plateTypeId);
    }

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Order.asc("name"))
        );
    }

    private PageResult<PlateType> mapPage(Page<PlateTypeEntity> page, PageQuery pageQuery) {
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }
}
