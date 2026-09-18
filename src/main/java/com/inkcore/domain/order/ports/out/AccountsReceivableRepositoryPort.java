package com.inkcore.domain.order.ports.out;

import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.List;
import java.util.Optional;

public interface AccountsReceivableRepositoryPort {

    Optional<AccountsReceivable> findByProductionOrderId(String companyId, String productionOrderId);

    List<AccountsReceivable> findByClientId(String companyId, String clientId);

    PageResult<AccountsReceivable> findPage(
            String companyId,
            String status,
            String clientId,
            PageQuery pageQuery
    );
}
