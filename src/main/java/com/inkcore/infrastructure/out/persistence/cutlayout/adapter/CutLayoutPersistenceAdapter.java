package com.inkcore.infrastructure.out.persistence.cutlayout.adapter;

import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.cutlayout.entity.CutLayoutEntity;
import com.inkcore.infrastructure.out.persistence.cutlayout.mapper.CutLayoutPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.cutlayout.repository.JpaCutLayoutRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class CutLayoutPersistenceAdapter implements CutLayoutRepositoryPort {

    private final JpaCutLayoutRepository jpaCutLayoutRepository;
    private final CutLayoutPersistenceMapper mapper;

    public CutLayoutPersistenceAdapter(
            JpaCutLayoutRepository jpaCutLayoutRepository,
            CutLayoutPersistenceMapper mapper
    ) {
        this.jpaCutLayoutRepository = jpaCutLayoutRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public CutLayout save(CutLayout cutLayout) {
        CutLayoutEntity existing = jpaCutLayoutRepository.findById(cutLayout.getCutLayoutId()).orElse(null);
        if (existing == null) {
            CutLayoutEntity entity = mapper.toNewEntity(cutLayout);
            CutLayoutEntity saved = jpaCutLayoutRepository.save(entity);
            return mapper.toDomain(saved);
        }
        mapper.copyScalars(cutLayout, existing);
        CutLayoutEntity saved = jpaCutLayoutRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CutLayout> findById(String cutLayoutId) {
        return jpaCutLayoutRepository.findById(cutLayoutId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CutLayout> findPage(PageQuery pageQuery) {
        return mapPage(jpaCutLayoutRepository.findAll(pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CutLayout> findPageByState(boolean state, PageQuery pageQuery) {
        return mapPage(jpaCutLayoutRepository.findAllByState(state, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CutLayout> findPageByCompanyId(String companyId, PageQuery pageQuery) {
        return mapPage(jpaCutLayoutRepository.findAllByCompanyId(companyId, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CutLayout> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery) {
        return mapPage(
                jpaCutLayoutRepository.findAllByCompanyIdAndState(companyId, state, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name) {
        return companyId != null
                && name != null
                && jpaCutLayoutRepository.existsByCompanyIdAndNameIgnoreCase(companyId.trim(), name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCaseExcludingCutLayoutId(
            String companyId,
            String name,
            String cutLayoutId
    ) {
        return companyId != null
                && name != null
                && cutLayoutId != null
                && jpaCutLayoutRepository.existsByCompanyIdAndNameIgnoreCaseAndCutLayoutIdNot(
                companyId.trim(), name.trim(), cutLayoutId);
    }

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Order.asc("name"))
        );
    }

    private PageResult<CutLayout> mapPage(Page<CutLayoutEntity> page, PageQuery pageQuery) {
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }
}
