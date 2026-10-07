package com.inkcore.infrastructure.out.persistence.paper.adapter;

import com.inkcore.domain.paper.model.PaperRemnant;
import com.inkcore.domain.paper.ports.out.PaperRemnantRepositoryPort;
import com.inkcore.infrastructure.out.persistence.paper.entity.PaperRemnantEntity;
import com.inkcore.infrastructure.out.persistence.paper.mapper.PaperPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.paper.repository.JpaPaperRemnantRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PaperRemnantPersistenceAdapter implements PaperRemnantRepositoryPort {

    private final JpaPaperRemnantRepository repository;
    private final PaperPersistenceMapper mapper;

    public PaperRemnantPersistenceAdapter(JpaPaperRemnantRepository repository, PaperPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PaperRemnant save(PaperRemnant remnant) {
        PaperRemnantEntity existing = repository.findById(remnant.getPaperRemnantId()).orElse(null);
        PaperRemnantEntity entity = existing == null ? mapper.toNewRemnantEntity(remnant) : existing;
        if (existing != null) {
            mapper.copyRemnantScalars(remnant, existing);
        }
        return mapper.toRemnantDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaperRemnant> findById(String paperRemnantId) {
        return repository.findById(paperRemnantId).map(mapper::toRemnantDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperRemnant> findByPaperId(String companyId, String paperId, Boolean state) {
        List<PaperRemnantEntity> entities = state == null
                ? repository.findAllByCompanyIdAndPaperIdOrderByEntryDateDesc(companyId, paperId)
                : repository.findAllByCompanyIdAndPaperIdAndStateOrderByEntryDateDesc(companyId, paperId, state);
        return entities.stream().map(mapper::toRemnantDomain).toList();
    }

    @Override
    @Transactional
    public void deleteById(String paperRemnantId) {
        repository.deleteById(paperRemnantId);
    }
}
