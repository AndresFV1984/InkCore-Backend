package com.inkcore.infrastructure.out.persistence.paper.adapter;

import com.inkcore.domain.paper.model.PaperCutLayout;
import com.inkcore.domain.paper.ports.out.PaperCutLayoutRepositoryPort;
import com.inkcore.infrastructure.out.persistence.paper.entity.PaperCutLayoutEntity;
import com.inkcore.infrastructure.out.persistence.paper.mapper.PaperPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.paper.repository.JpaPaperCutLayoutRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PaperCutLayoutPersistenceAdapter implements PaperCutLayoutRepositoryPort {

    private final JpaPaperCutLayoutRepository repository;
    private final PaperPersistenceMapper mapper;

    public PaperCutLayoutPersistenceAdapter(JpaPaperCutLayoutRepository repository, PaperPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PaperCutLayout save(PaperCutLayout layout) {
        PaperCutLayoutEntity existing = repository.findById(layout.getPaperCutLayoutId()).orElse(null);
        PaperCutLayoutEntity entity = existing == null ? mapper.toNewCutLayoutEntity(layout) : existing;
        if (existing != null) {
            mapper.copyCutLayoutScalars(layout, existing);
        }
        return mapper.toCutLayoutDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaperCutLayout> findById(String paperCutLayoutId) {
        return repository.findById(paperCutLayoutId).map(mapper::toCutLayoutDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperCutLayout> findByPaperId(String companyId, String paperId) {
        return repository.findAllByCompanyIdAndPaperIdOrderByCreatedAtAsc(companyId, paperId).stream()
                .map(mapper::toCutLayoutDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(String paperCutLayoutId) {
        repository.deleteById(paperCutLayoutId);
    }
}
