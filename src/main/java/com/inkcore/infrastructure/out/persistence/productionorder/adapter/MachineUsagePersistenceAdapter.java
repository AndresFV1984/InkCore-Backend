package com.inkcore.infrastructure.out.persistence.productionorder.adapter;

import com.inkcore.domain.productionorder.model.MachineUsage;
import com.inkcore.domain.productionorder.ports.out.MachineUsageRepositoryPort;
import com.inkcore.infrastructure.out.persistence.productionorder.entity.ProductionOrderMachineUsageEntity;
import com.inkcore.infrastructure.out.persistence.productionorder.repository.JpaProductionOrderMachineUsageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MachineUsagePersistenceAdapter implements MachineUsageRepositoryPort {

    private final JpaProductionOrderMachineUsageRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public MachineUsagePersistenceAdapter(JpaProductionOrderMachineUsageRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MachineUsage> findByProductionOrderId(String companyId, String productionOrderId) {
        return repository
                .findAllByCompanyIdAndProductionOrderIdOrderByPhaseAscCreatedAtAsc(companyId, productionOrderId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MachineUsage> findByProductionOrderIdAndPhase(
            String companyId,
            String productionOrderId,
            String phase
    ) {
        return repository
                .findAllByCompanyIdAndProductionOrderIdAndPhase(companyId, productionOrderId, phase)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByProductionOrderIdAndPhase(String productionOrderId, String phase) {
        repository.deleteByProductionOrderIdAndPhase(productionOrderId, phase);
        entityManager.flush();
    }

    @Override
    @Transactional
    public void deleteByProductionOrderIdAndPhaseNot(String productionOrderId, String phase) {
        repository.deleteByProductionOrderIdAndPhaseNot(productionOrderId, phase);
        entityManager.flush();
    }

    @Override
    @Transactional
    public MachineUsage save(MachineUsage usage) {
        ProductionOrderMachineUsageEntity existing = repository.findById(usage.getMachineUsageId()).orElse(null);
        ProductionOrderMachineUsageEntity entity = existing == null ? toNewEntity(usage) : existing;
        if (existing != null) {
            entity.setEstimatedSetupMinutes(usage.getEstimatedSetupMinutes());
            entity.setEstimatedRunMinutes(usage.getEstimatedRunMinutes());
            entity.setActualSetupMinutes(usage.getActualSetupMinutes());
            entity.setActualRunMinutes(usage.getActualRunMinutes());
            entity.setUpdatedAt(usage.getUpdatedAt());
        }
        ProductionOrderMachineUsageEntity stored = repository.save(entity);
        entityManager.flush();
        entityManager.refresh(stored);
        return toDomain(stored);
    }

    @Override
    @Transactional
    public List<MachineUsage> replacePhase(String productionOrderId, String phase, List<MachineUsage> usages) {
        String companyId = usages.isEmpty() ? null : usages.get(0).getCompanyId();
        List<ProductionOrderMachineUsageEntity> previous = companyId == null
                ? List.of()
                : repository.findAllByCompanyIdAndProductionOrderIdAndPhase(companyId, productionOrderId, phase);
        Map<String, ProductionOrderMachineUsageEntity> byMachine = new LinkedHashMap<>();
        for (ProductionOrderMachineUsageEntity entity : previous) {
            byMachine.put(entity.getMachineId(), entity);
        }
        repository.deleteByProductionOrderIdAndPhase(productionOrderId, phase);
        entityManager.flush();

        List<MachineUsage> saved = new ArrayList<>();
        for (MachineUsage usage : usages) {
            ProductionOrderMachineUsageEntity prior = byMachine.get(usage.getMachineId());
            if (prior != null) {
                if (usage.getActualSetupMinutes() == null) {
                    usage.setActualSetupMinutes(prior.getActualSetupMinutes());
                }
                if (usage.getActualRunMinutes() == null) {
                    usage.setActualRunMinutes(prior.getActualRunMinutes());
                }
                if (usage.getCreatedAt() == null) {
                    usage.setCreatedAt(prior.getCreatedAt());
                }
            }
            ProductionOrderMachineUsageEntity entity = toNewEntity(usage);
            ProductionOrderMachineUsageEntity stored = repository.save(entity);
            entityManager.flush();
            entityManager.refresh(stored);
            saved.add(toDomain(stored));
        }
        return saved;
    }

    private ProductionOrderMachineUsageEntity toNewEntity(MachineUsage usage) {
        ProductionOrderMachineUsageEntity entity = new ProductionOrderMachineUsageEntity();
        entity.setProductionOrderMachineUsageId(usage.getMachineUsageId());
        entity.setCompanyId(usage.getCompanyId());
        entity.setProductionOrderId(usage.getProductionOrderId());
        entity.setPhase(usage.getPhase());
        entity.setMachineId(usage.getMachineId());
        entity.setMachineNameSnapshot(usage.getMachineNameSnapshot());
        entity.setCostPerHourSnapshot(usage.getCostPerHourSnapshot());
        entity.setEstimatedSetupMinutes(usage.getEstimatedSetupMinutes());
        entity.setEstimatedRunMinutes(usage.getEstimatedRunMinutes());
        entity.setActualSetupMinutes(usage.getActualSetupMinutes());
        entity.setActualRunMinutes(usage.getActualRunMinutes());
        entity.setCreatedAt(usage.getCreatedAt());
        entity.setUpdatedAt(usage.getUpdatedAt());
        return entity;
    }

    private MachineUsage toDomain(ProductionOrderMachineUsageEntity entity) {
        MachineUsage usage = new MachineUsage();
        usage.setMachineUsageId(entity.getProductionOrderMachineUsageId());
        usage.setCompanyId(entity.getCompanyId());
        usage.setProductionOrderId(entity.getProductionOrderId());
        usage.setPhase(entity.getPhase());
        usage.setMachineId(entity.getMachineId());
        usage.setMachineNameSnapshot(entity.getMachineNameSnapshot());
        usage.setCostPerHourSnapshot(entity.getCostPerHourSnapshot());
        usage.setEstimatedSetupMinutes(entity.getEstimatedSetupMinutes());
        usage.setEstimatedRunMinutes(entity.getEstimatedRunMinutes());
        usage.setEstimatedMachineCost(entity.getEstimatedMachineCost());
        usage.setActualSetupMinutes(entity.getActualSetupMinutes());
        usage.setActualRunMinutes(entity.getActualRunMinutes());
        usage.setActualMachineCost(entity.getActualMachineCost());
        usage.setCreatedAt(entity.getCreatedAt());
        usage.setUpdatedAt(entity.getUpdatedAt());
        return usage;
    }
}
