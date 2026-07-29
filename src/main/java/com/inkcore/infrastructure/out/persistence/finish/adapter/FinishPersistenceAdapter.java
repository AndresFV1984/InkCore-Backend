package com.inkcore.infrastructure.out.persistence.finish.adapter;

import com.inkcore.domain.finish.model.Finish;
import com.inkcore.domain.finish.ports.out.FinishRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.finish.entity.FinishEntity;
import com.inkcore.infrastructure.out.persistence.finish.mapper.FinishPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.finish.repository.JpaFinishRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class FinishPersistenceAdapter implements FinishRepositoryPort {

    private final JpaFinishRepository jpaFinishRepository;
    private final FinishPersistenceMapper mapper;

    public FinishPersistenceAdapter(
            JpaFinishRepository jpaFinishRepository,
            FinishPersistenceMapper mapper
    ) {
        this.jpaFinishRepository = jpaFinishRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Finish save(Finish finish) {
        FinishEntity existing = jpaFinishRepository.findById(finish.getFinishId()).orElse(null);
        if (existing == null) {
            FinishEntity entity = mapper.toNewEntity(finish);
            FinishEntity saved = jpaFinishRepository.save(entity);
            return mapper.toDomain(saved);
        }
        mapper.copyScalars(finish, existing);
        FinishEntity saved = jpaFinishRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Finish> findById(String finishId) {
        return jpaFinishRepository.findById(finishId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Finish> findPage(PageQuery pageQuery) {
        return mapPage(jpaFinishRepository.findAll(pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Finish> findPageByState(boolean state, PageQuery pageQuery) {
        return mapPage(jpaFinishRepository.findAllByState(state, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Finish> findPageByCompanyId(String companyId, PageQuery pageQuery) {
        return mapPage(jpaFinishRepository.findAllByCompanyId(companyId, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Finish> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery) {
        return mapPage(
                jpaFinishRepository.findAllByCompanyIdAndState(companyId, state, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name) {
        return companyId != null
                && name != null
                && jpaFinishRepository.existsByCompanyIdAndNameIgnoreCase(companyId.trim(), name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCaseExcludingFinishId(
            String companyId,
            String name,
            String finishId
    ) {
        return companyId != null
                && name != null
                && finishId != null
                && jpaFinishRepository.existsByCompanyIdAndNameIgnoreCaseAndFinishIdNot(
                companyId.trim(), name.trim(), finishId);
    }

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Order.desc("quickAccess"), Sort.Order.asc("name"))
        );
    }

    private PageResult<Finish> mapPage(Page<FinishEntity> page, PageQuery pageQuery) {
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }
}
