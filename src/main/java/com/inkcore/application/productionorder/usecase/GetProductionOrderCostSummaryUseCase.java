package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.model.CostSummary;
import com.inkcore.domain.productionorder.model.WasteRecord;
import com.inkcore.domain.productionorder.ports.out.CostSummaryRepositoryPort;
import com.inkcore.domain.productionorder.ports.out.WasteRecordRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetProductionOrderCostSummaryUseCase {

    private final ProductionOrderSupport support;
    private final ProductionOrderCostingCoordinator costing;
    private final CostSummaryRepositoryPort costSummaryRepository;
    private final WasteRecordRepositoryPort wasteRecordRepository;

    public GetProductionOrderCostSummaryUseCase(
            ProductionOrderSupport support,
            ProductionOrderCostingCoordinator costing,
            CostSummaryRepositoryPort costSummaryRepository,
            WasteRecordRepositoryPort wasteRecordRepository
    ) {
        this.support = support;
        this.costing = costing;
        this.costSummaryRepository = costSummaryRepository;
        this.wasteRecordRepository = wasteRecordRepository;
    }

    @Transactional
    public ProductionOrderCostRead execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        var order = support.requireOrder(productionOrderId, companyId);
        costing.syncQuotedPrice(order);
        CostSummary summary = costSummaryRepository.findByProductionOrderId(companyId, productionOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "COST_SUMMARY_NOT_FOUND",
                        "La orden aún no tiene resumen de costos"
                ));
        List<WasteRecord> desperdicios = wasteRecordRepository.findByProductionOrderId(companyId, productionOrderId)
                .stream()
                .filter(record -> "desperdicio".equals(record.getWasteCategory()))
                .toList();
        return new ProductionOrderCostRead(summary, desperdicios);
    }

    public record ProductionOrderCostRead(CostSummary summary, List<WasteRecord> desperdicios) {
    }
}
