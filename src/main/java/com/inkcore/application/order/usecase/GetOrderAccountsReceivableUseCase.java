package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.AccountsReceivableAging;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class GetOrderAccountsReceivableUseCase {

    private static final String STATUS_WITHOUT_MOVEMENTS = "sin_movimientos";

    private final OrderSupport support;
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;

    public GetOrderAccountsReceivableUseCase(
            OrderSupport support,
            AccountsReceivableRepositoryPort accountsReceivableRepository
    ) {
        this.support = support;
        this.accountsReceivableRepository = accountsReceivableRepository;
    }

    @Transactional(readOnly = true)
    public AccountsReceivableView execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        ProductionOrder order = support.requireActiveOrder(productionOrderId, companyId);

        return accountsReceivableRepository.findByProductionOrderId(companyId, productionOrderId)
                .map(summary -> from(summary, true))
                .orElseGet(() -> withoutMovements(order));
    }

    private static AccountsReceivableView from(AccountsReceivable summary, boolean hasMovements) {
        AccountsReceivableAging.AgingSnapshot aging = AccountsReceivableAging.of(summary, LocalDate.now());
        return new AccountsReceivableView(
                summary.getAccountsReceivableId(),
                summary.getCxcNumber(),
                summary.getProductionOrderId(),
                summary.getClientId(),
                summary.getTotalUnits(),
                summary.getDeliveredUnits(),
                summary.getPendingUnits(),
                summary.getTotalOwed(),
                summary.getTotalPaid(),
                summary.getTotalRemaining(),
                nullToZero(summary.getTotalCashPaid()),
                nullToZero(summary.getTotalWithheld()),
                nullToZero(summary.getTotalAdvancePaid()),
                summary.getStatus() == null ? null : summary.getStatus().getDbValue(),
                hasMovements,
                summary.getOpenedAt(),
                summary.getDueDate(),
                summary.getPaymentTermDays(),
                aging.collectionStatus(),
                aging.daysOverdue(),
                aging.agingBucket(),
                aging.dueSoon(),
                aging.overdue(),
                summary.getLastDeliveryAt(),
                summary.getLastPaymentNumber(),
                summary.getLastPaymentAt()
        );
    }

    private static AccountsReceivableView withoutMovements(ProductionOrder order) {
        return new AccountsReceivableView(
                null,
                null,
                order.getProductionOrderId(),
                order.getClientId(),
                order.getRequestedQuantity(),
                0,
                order.getRequestedQuantity(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                STATUS_WITHOUT_MOVEMENTS,
                false,
                null,
                null,
                0,
                "sin_vencimiento",
                0,
                "current",
                false,
                false,
                null,
                null,
                null
        );
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record AccountsReceivableView(
            String accountsReceivableId,
            String cxcNumber,
            String productionOrderId,
            String clientId,
            int totalUnits,
            int deliveredUnits,
            int pendingUnits,
            BigDecimal totalOwed,
            BigDecimal totalPaid,
            BigDecimal totalRemaining,
            BigDecimal totalCashPaid,
            BigDecimal totalWithheld,
            BigDecimal totalAdvancePaid,
            String status,
            boolean hasMovements,
            LocalDateTime openedAt,
            LocalDate dueDate,
            int paymentTermDays,
            String collectionStatus,
            int daysOverdue,
            String agingBucket,
            boolean dueSoon,
            boolean overdue,
            LocalDateTime lastDeliveryAt,
            String lastPaymentNumber,
            LocalDateTime lastPaymentAt
    ) {
    }
}
