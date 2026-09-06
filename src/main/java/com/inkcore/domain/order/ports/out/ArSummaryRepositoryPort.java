package com.inkcore.domain.order.ports.out;

import com.inkcore.domain.order.model.ArSummary;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.Optional;

public interface ArSummaryRepositoryPort {

    Optional<ArSummary> findByProductionOrderId(String companyId, String productionOrderId);

    PageResult<ArSummary> findPage(
            String companyId,
            String status,
            String clientId,
            PageQuery pageQuery
    );
}
