package com.inkcore.domain.productionorder.ports.out;

import com.inkcore.domain.productionorder.model.WasteRecord;

import java.util.List;

public interface WasteRecordRepositoryPort {

    List<WasteRecord> findByProductionOrderId(String companyId, String productionOrderId);

    List<WasteRecord> findByProductionOrderPhaseAndCategory(
            String companyId,
            String productionOrderId,
            String phase,
            String wasteCategory
    );

    void deleteByProductionOrderId(String productionOrderId);

    void deleteByIds(List<String> wasteRecordIds);

    WasteRecord save(WasteRecord record);
}
