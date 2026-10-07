package com.inkcore.infrastructure.out.persistence.paper.adapter;

import com.inkcore.domain.paper.model.PaperStock;
import com.inkcore.domain.paper.ports.out.PaperStockRepositoryPort;
import com.inkcore.infrastructure.out.persistence.paper.entity.PaperStockEntity;
import com.inkcore.infrastructure.out.persistence.paper.mapper.PaperPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.paper.repository.JpaPaperStockRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PaperStockPersistenceAdapter implements PaperStockRepositoryPort {

    private final JpaPaperStockRepository repository;
    private final PaperPersistenceMapper mapper;

    public PaperStockPersistenceAdapter(JpaPaperStockRepository repository, PaperPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PaperStock save(PaperStock stock) {
        PaperStockEntity existing = repository.findById(stock.getPaperStockId()).orElse(null);
        PaperStockEntity entity = existing == null ? mapper.toNewStockEntity(stock) : existing;
        if (existing != null) {
            mapper.copyStockScalars(stock, existing);
        }
        return mapper.toStockDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaperStock> findById(String paperStockId) {
        return repository.findById(paperStockId).map(mapper::toStockDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperStock> findByPaperId(String companyId, String paperId, Boolean state) {
        List<PaperStockEntity> entities = state == null
                ? repository.findAllByCompanyIdAndPaperIdOrderByEntryDateDesc(companyId, paperId)
                : repository.findAllByCompanyIdAndPaperIdAndStateOrderByEntryDateDesc(companyId, paperId, state);
        return entities.stream().map(mapper::toStockDomain).toList();
    }

    @Override
    @Transactional
    public void deleteById(String paperStockId) {
        repository.deleteById(paperStockId);
    }
}
