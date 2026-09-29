package com.inkcore.domain.productionorder.ports.out;

import com.inkcore.domain.productionorder.model.CostSummary;
import com.inkcore.domain.productionorder.model.ProfitabilityRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CostSummaryRepositoryPort {

    Optional<CostSummary> findByProductionOrderId(String companyId, String productionOrderId);

    void upsertQuotedPrice(String productionOrderId, String companyId, BigDecimal quotedPrice);

    List<ProfitabilityRow> findProfitability(
            String companyId,
            LocalDate from,
            LocalDate to,
            String clientId,
            String sellerId
    );
}
