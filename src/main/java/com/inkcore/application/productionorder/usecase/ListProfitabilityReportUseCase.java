package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.model.ProfitabilityRow;
import com.inkcore.domain.productionorder.ports.out.CostSummaryRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ListProfitabilityReportUseCase {

    private final ProductionOrderSupport support;
    private final CostSummaryRepositoryPort costSummaryRepository;

    public ListProfitabilityReportUseCase(
            ProductionOrderSupport support,
            CostSummaryRepositoryPort costSummaryRepository
    ) {
        this.support = support;
        this.costSummaryRepository = costSummaryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProfitabilityRow> execute(
            LocalDate from,
            LocalDate to,
            String clientId,
            String sellerId,
            Authentication authentication
    ) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from y to son obligatorios");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to no puede ser anterior a from");
        }
        String companyId = support.companyId(authentication);
        return costSummaryRepository.findProfitability(
                companyId,
                from,
                to,
                blankToNull(clientId),
                blankToNull(sellerId)
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
