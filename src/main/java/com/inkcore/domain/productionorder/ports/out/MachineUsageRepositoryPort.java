package com.inkcore.domain.productionorder.ports.out;

import com.inkcore.domain.productionorder.model.MachineUsage;

import java.util.List;

public interface MachineUsageRepositoryPort {

    List<MachineUsage> findByProductionOrderId(String companyId, String productionOrderId);

    List<MachineUsage> findByProductionOrderIdAndPhase(String companyId, String productionOrderId, String phase);

    void deleteByProductionOrderIdAndPhase(String productionOrderId, String phase);

    void deleteByProductionOrderIdAndPhaseNot(String productionOrderId, String phase);

    MachineUsage save(MachineUsage usage);

    List<MachineUsage> replacePhase(String productionOrderId, String phase, List<MachineUsage> usages);
}
