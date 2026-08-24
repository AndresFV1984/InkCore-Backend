package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderFilter;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ListProductionOrdersUseCase {

    private final ProductionOrderSupport support;

    public ListProductionOrdersUseCase(ProductionOrderSupport support) {
        this.support = support;
    }

    @Transactional(readOnly = true)
    public PageResult<ProductionOrder> execute(
            String status,
            String clientId,
            String orderNumber,
            LocalDate fromDate,
            LocalDate toDate,
            Boolean state,
            PageQuery pageQuery,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        ProductionOrderFilter filter = new ProductionOrderFilter(
                companyId,
                blankToNull(status),
                blankToNull(clientId),
                blankToNull(orderNumber),
                fromDate,
                toDate,
                state
        );
        return support.repository().findPage(filter, pageQuery);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
