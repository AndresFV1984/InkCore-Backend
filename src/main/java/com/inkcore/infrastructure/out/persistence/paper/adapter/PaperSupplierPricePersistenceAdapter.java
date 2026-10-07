package com.inkcore.infrastructure.out.persistence.paper.adapter;

import com.inkcore.domain.paper.model.PaperPriceHistory;
import com.inkcore.domain.paper.model.PaperSupplierPrice;
import com.inkcore.domain.paper.ports.out.PaperSupplierPriceRepositoryPort;
import com.inkcore.infrastructure.out.persistence.paper.entity.PaperSupplierPriceEntity;
import com.inkcore.infrastructure.out.persistence.paper.mapper.PaperPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.paper.repository.JpaPaperPriceHistoryRepository;
import com.inkcore.infrastructure.out.persistence.paper.repository.JpaPaperSupplierPriceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaperSupplierPricePersistenceAdapter implements PaperSupplierPriceRepositoryPort {

    private final JpaPaperSupplierPriceRepository priceRepository;
    private final JpaPaperPriceHistoryRepository historyRepository;
    private final PaperPersistenceMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public PaperSupplierPricePersistenceAdapter(
            JpaPaperSupplierPriceRepository priceRepository,
            JpaPaperPriceHistoryRepository historyRepository,
            PaperPersistenceMapper mapper
    ) {
        this.priceRepository = priceRepository;
        this.historyRepository = historyRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperSupplierPrice> findByPaperId(String companyId, String paperId) {
        return priceRepository.findAllByCompanyIdAndPaperIdOrderBySupplierIdAsc(companyId, paperId).stream()
                .map(mapper::toPriceDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperPriceHistory> findHistoryByPaperId(String companyId, String paperId) {
        return historyRepository.findAllByCompanyIdAndPaperIdOrderByEffectiveFromDesc(companyId, paperId)
                .stream()
                .map(mapper::toPriceHistoryDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<PaperSupplierPrice> replaceDiff(
            String companyId,
            String paperId,
            List<PaperSupplierPrice> desiredPrices,
            String changedBy
    ) {
        bindSession(changedBy);
        List<PaperSupplierPrice> desired = desiredPrices == null ? List.of() : desiredPrices;
        Map<String, PaperSupplierPriceEntity> existingBySupplier = priceRepository
                .findAllByCompanyIdAndPaperIdOrderBySupplierIdAsc(companyId, paperId)
                .stream()
                .collect(Collectors.toMap(
                        PaperSupplierPriceEntity::getSupplierId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        Map<String, PaperSupplierPrice> desiredBySupplier = desired.stream()
                .collect(Collectors.toMap(
                        PaperSupplierPrice::getSupplierId, Function.identity(), (a, b) -> b, LinkedHashMap::new));

        List<String> toDelete = existingBySupplier.keySet().stream()
                .filter(supplierId -> !desiredBySupplier.containsKey(supplierId))
                .toList();
        if (!toDelete.isEmpty()) {
            priceRepository.deleteAllByCompanyIdAndPaperIdAndSupplierIdIn(companyId, paperId, toDelete);
        }

        List<PaperSupplierPriceEntity> saved = new ArrayList<>();
        for (PaperSupplierPrice price : desiredBySupplier.values()) {
            PaperSupplierPriceEntity entity = existingBySupplier.get(price.getSupplierId());
            if (entity == null) {
                entity = mapper.toNewPriceEntity(price);
            } else {
                mapper.copyPriceScalars(price, entity);
            }
            saved.add(priceRepository.save(entity));
        }
        entityManager.flush();
        saved.forEach(e -> entityManager.refresh(e));
        return saved.stream().map(mapper::toPriceDomain).toList();
    }

    private void bindSession(String changedBy) {
        entityManager.createNativeQuery("SELECT set_config('inkcore.changed_by', :changedBy, true)")
                .setParameter("changedBy", changedBy == null ? "" : changedBy)
                .getSingleResult();
    }
}
