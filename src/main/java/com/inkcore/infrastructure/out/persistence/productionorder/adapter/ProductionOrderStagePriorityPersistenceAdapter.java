package com.inkcore.infrastructure.out.persistence.productionorder.adapter;

import com.inkcore.domain.productionorder.ports.out.ProductionOrderStagePriorityRepositoryPort;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderStagePriorityEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderStagePriorityRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class ProductionOrderStagePriorityPersistenceAdapter implements ProductionOrderStagePriorityRepositoryPort {

    private final JpaProductionOrderStagePriorityRepository repository;

    public ProductionOrderStagePriorityPersistenceAdapter(JpaProductionOrderStagePriorityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StagePriority> findByCompany(String companyId) {
        if (companyId == null || companyId.isBlank()) {
            return List.of();
        }
        return repository.findByCompanyIdOrderByStageCodeAscPriorityRankAsc(companyId.trim()).stream()
                .map(row -> new StagePriority(row.getProductionOrderId(), row.getStageCode(), row.getPriorityRank()))
                .toList();
    }

    @Override
    @Transactional
    public void replaceStageQueue(
            String companyId,
            String stageCode,
            List<String> orderedProductionOrderIds,
            String updatedBy,
            LocalDateTime updatedAt
    ) {
        String company = companyId.trim();
        repository.deleteByCompanyIdAndStageCode(company, stageCode);
        repository.flush();
        int rank = 1;
        for (String productionOrderId : orderedProductionOrderIds) {
            ProductionOrderStagePriorityEntity row = new ProductionOrderStagePriorityEntity();
            row.setStagePriorityId(UUID.randomUUID().toString());
            row.setCompanyId(company);
            row.setProductionOrderId(productionOrderId);
            row.setStageCode(stageCode);
            row.setPriorityRank(rank);
            row.setCreatedAt(updatedAt);
            row.setUpdatedAt(updatedAt);
            row.setCreatedBy(updatedBy);
            row.setUpdatedBy(updatedBy);
            repository.save(row);
            rank++;
        }
    }
}
