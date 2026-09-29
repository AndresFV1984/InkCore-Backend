package com.inkcore.infrastructure.out.persistence.productionorder.adapter;

import com.inkcore.domain.productionorder.model.WasteRecord;
import com.inkcore.domain.productionorder.ports.out.WasteRecordRepositoryPort;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderWasteRecordEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderWasteRecordRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class WasteRecordPersistenceAdapter implements WasteRecordRepositoryPort {

    private final JpaProductionOrderWasteRecordRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public WasteRecordPersistenceAdapter(JpaProductionOrderWasteRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WasteRecord> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository
                .findAllByCompanyIdAndProductionOrderIdOrderByCreatedAtAsc(companyId, productionOrderId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WasteRecord> findByProductionOrderPhaseAndCategory(
            String companyId,
            String productionOrderId,
            String phase,
            String wasteCategory
    ) {
        return repository
                .findAllByCompanyIdAndProductionOrderIdAndPhaseAndWasteCategory(
                        companyId, productionOrderId, phase, wasteCategory)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByProductionOrderId(String productionOrderId) {
        repository.deleteByProductionOrderId(productionOrderId);
        entityManager.flush();
    }

    @Override
    @Transactional
    public void deleteByIds(List<String> wasteRecordIds) {
        if (wasteRecordIds == null || wasteRecordIds.isEmpty()) {
            return;
        }
        repository.deleteAllById(wasteRecordIds);
        entityManager.flush();
    }

    @Override
    @Transactional
    public WasteRecord save(WasteRecord record) {
        ProductionOrderWasteRecordEntity existing = repository.findById(record.getWasteRecordId()).orElse(null);
        ProductionOrderWasteRecordEntity entity = existing == null ? new ProductionOrderWasteRecordEntity() : existing;
        copy(record, entity);
        ProductionOrderWasteRecordEntity stored = repository.save(entity);
        entityManager.flush();
        entityManager.refresh(stored);
        return toDomain(stored);
    }

    private void copy(WasteRecord record, ProductionOrderWasteRecordEntity entity) {
        entity.setProductionOrderWasteRecordId(record.getWasteRecordId());
        entity.setCompanyId(record.getCompanyId());
        entity.setProductionOrderId(record.getProductionOrderId());
        entity.setPhase(record.getPhase());
        entity.setWasteCategory(record.getWasteCategory());
        entity.setWasteOrigin(record.getWasteOrigin() == null ? "exceso" : record.getWasteOrigin());
        entity.setMaterialType(record.getMaterialType());
        entity.setPaperRowId(record.getPaperRowId());
        entity.setPostpressLineId(record.getPostpressLineId());
        entity.setPlannedQuantity(record.getPlannedQuantity());
        entity.setActualQuantity(record.getActualQuantity());
        entity.setUnitCostSnapshot(record.getUnitCostSnapshot());
        entity.setNote(record.getNote());
        entity.setCreatedAt(record.getCreatedAt());
        entity.setUpdatedAt(record.getUpdatedAt());
    }

    private WasteRecord toDomain(ProductionOrderWasteRecordEntity entity) {
        WasteRecord record = new WasteRecord();
        record.setWasteRecordId(entity.getProductionOrderWasteRecordId());
        record.setCompanyId(entity.getCompanyId());
        record.setProductionOrderId(entity.getProductionOrderId());
        record.setPhase(entity.getPhase());
        record.setWasteCategory(entity.getWasteCategory());
        record.setWasteOrigin(entity.getWasteOrigin() == null ? "exceso" : entity.getWasteOrigin());
        record.setMaterialType(entity.getMaterialType());
        record.setPaperRowId(entity.getPaperRowId());
        record.setPostpressLineId(entity.getPostpressLineId());
        record.setPlannedQuantity(entity.getPlannedQuantity());
        record.setActualQuantity(entity.getActualQuantity());
        record.setUnitCostSnapshot(entity.getUnitCostSnapshot());
        record.setPlannedCost(entity.getPlannedCost());
        record.setActualCost(entity.getActualCost());
        record.setNote(entity.getNote());
        record.setCreatedAt(entity.getCreatedAt());
        record.setUpdatedAt(entity.getUpdatedAt());
        return record;
    }
}
