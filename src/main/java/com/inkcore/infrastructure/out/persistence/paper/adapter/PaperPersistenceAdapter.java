package com.inkcore.infrastructure.out.persistence.paper.adapter;

import com.inkcore.domain.paper.model.Paper;
import com.inkcore.domain.paper.ports.out.PaperRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.paper.entity.PaperEntity;
import com.inkcore.infrastructure.out.persistence.paper.mapper.PaperPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.paper.repository.JpaPaperRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class PaperPersistenceAdapter implements PaperRepositoryPort {

    private final JpaPaperRepository repository;
    private final PaperPersistenceMapper mapper;

    public PaperPersistenceAdapter(JpaPaperRepository repository, PaperPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Paper save(Paper paper) {
        PaperEntity existing = repository.findById(paper.getPaperId()).orElse(null);
        PaperEntity entity = existing == null ? mapper.toNewPaperEntity(paper) : existing;
        if (existing != null) {
            mapper.copyPaperScalars(paper, existing);
        }
        return mapper.toPaperDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Paper> findById(String paperId) {
        return repository.findById(paperId).map(mapper::toPaperDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Paper> findPage(
            String companyId,
            Boolean state,
            Boolean coated,
            Boolean acceptsRemnants,
            PageQuery pageQuery
    ) {
        PageRequest pageable = PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Order.asc("name"))
        );
        Page<PaperEntity> page = repository.findPage(companyId, state, coated, acceptsRemnants, pageable);
        return new PageResult<>(
                page.getContent().stream().map(mapper::toPaperDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsDuplicate(
            String companyId,
            String name,
            BigDecimal grammage,
            BigDecimal width,
            BigDecimal height,
            String unit
    ) {
        return repository.existsDuplicate(companyId, name, grammage, width, height, unit);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsDuplicateExcludingId(
            String companyId,
            String name,
            BigDecimal grammage,
            BigDecimal width,
            BigDecimal height,
            String unit,
            String paperId
    ) {
        return repository.existsDuplicateExcludingId(companyId, name, grammage, width, height, unit, paperId);
    }
}
