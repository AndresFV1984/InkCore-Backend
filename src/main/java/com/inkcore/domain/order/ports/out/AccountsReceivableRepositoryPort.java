package com.inkcore.domain.order.ports.out;

import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.List;
import java.util.Optional;

public interface AccountsReceivableRepositoryPort {

    Optional<AccountsReceivable> findByProductionOrderId(String companyId, String productionOrderId);

    /**
     * Relee la fila AR desde BD tras un INSERT en order_payments/order_deliveries.
     * Los triggers actualizan total_paid/total_owed fuera del L1 de Hibernate; sin refresh
     * el snapshot del create puede devolver montos viejos (p. ej. totalPaid=0).
     */
    Optional<AccountsReceivable> findByProductionOrderIdFresh(String companyId, String productionOrderId);

    List<AccountsReceivable> findByProductionOrderIds(String companyId, List<String> productionOrderIds);

    List<AccountsReceivable> findByClientId(String companyId, String clientId);

    PageResult<AccountsReceivable> findPage(
            String companyId,
            String status,
            String clientId,
            PageQuery pageQuery
    );

    /**
     * Red de seguridad: crea shell ABN para customer_orders sin fila AR.
     * Preferir el trigger en INSERT de customer_orders; este método cubre históricos.
     */
    void openMissingBillableAccounts(String companyId);
}
