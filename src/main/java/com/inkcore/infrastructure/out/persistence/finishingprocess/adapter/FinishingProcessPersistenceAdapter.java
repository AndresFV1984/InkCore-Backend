package com.inkcore.infrastructure.out.persistence.finishingprocess.adapter;

import com.inkcore.domain.finishingprocess.model.FinishingProcess;
import com.inkcore.domain.finishingprocess.ports.out.FinishingProcessRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.finishingprocess.entity.FinishingProcessEntity;
import com.inkcore.infrastructure.out.persistence.finishingprocess.mapper.FinishingProcessPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.finishingprocess.repository.JpaFinishingProcessRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class FinishingProcessPersistenceAdapter implements FinishingProcessRepositoryPort {

    private final JpaFinishingProcessRepository jpaFinishingProcessRepository;
    private final FinishingProcessPersistenceMapper mapper;

    public FinishingProcessPersistenceAdapter(
            JpaFinishingProcessRepository jpaFinishingProcessRepository,
            FinishingProcessPersistenceMapper mapper
    ) {
        this.jpaFinishingProcessRepository = jpaFinishingProcessRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public FinishingProcess save(FinishingProcess finishingProcess) {
        FinishingProcessEntity existing = jpaFinishingProcessRepository
                .findById(finishingProcess.getFinishingProcessId())
                .orElse(null);
        if (existing == null) {
            FinishingProcessEntity entity = mapper.toNewEntity(finishingProcess);
            FinishingProcessEntity saved = jpaFinishingProcessRepository.save(entity);
            return mapper.toDomain(saved);
        }
        mapper.copyScalars(finishingProcess, existing);
        FinishingProcessEntity saved = jpaFinishingProcessRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FinishingProcess> findById(String finishingProcessId) {
        return jpaFinishingProcessRepository.findById(finishingProcessId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FinishingProcess> findPage(PageQuery pageQuery) {
        return mapPage(jpaFinishingProcessRepository.findAll(pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FinishingProcess> findPageByState(boolean state, PageQuery pageQuery) {
        return mapPage(jpaFinishingProcessRepository.findAllByState(state, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FinishingProcess> findPageByCompanyId(String companyId, PageQuery pageQuery) {
        return mapPage(
                jpaFinishingProcessRepository.findAllByCompanyId(companyId, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<FinishingProcess> findPageByCompanyIdAndState(
            String companyId,
            boolean state,
            PageQuery pageQuery
    ) {
        return mapPage(
                jpaFinishingProcessRepository.findAllByCompanyIdAndState(companyId, state, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name) {
        return companyId != null
                && name != null
                && jpaFinishingProcessRepository.existsByCompanyIdAndNameIgnoreCase(
                companyId.trim(), name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCaseExcludingFinishingProcessId(
            String companyId,
            String name,
            String finishingProcessId
    ) {
        return companyId != null
                && name != null
                && finishingProcessId != null
                && jpaFinishingProcessRepository.existsByCompanyIdAndNameIgnoreCaseAndFinishingProcessIdNot(
                companyId.trim(), name.trim(), finishingProcessId);
    }

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Order.desc("quickAccess"), Sort.Order.asc("name"))
        );
    }

    private PageResult<FinishingProcess> mapPage(Page<FinishingProcessEntity> page, PageQuery pageQuery) {
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }
}
