package com.inkcore.application.order.usecase;

import com.inkcore.application.order.AbonosBalance;
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
        String odpNumber = support.resolveOdpNumber(companyId, productionOrderId);

        return accountsReceivableRepository.findByProductionOrderId(companyId, productionOrderId)
                .map(summary -> from(summary, order, true, odpNumber))
                .orElseGet(() -> withoutMovements(order, odpNumber));
    }

    private static AccountsReceivableView from(
            AccountsReceivable summary,
            ProductionOrder order,
            boolean hasMovements,
            String odpNumber
    ) {
        AbonosBalance.applyTo(summary, order);
        AccountsReceivableAging.AgingSnapshot aging = AccountsReceivableAging.of(summary, LocalDate.now());
        return new AccountsReceivableView(
                summary.getAccountsReceivableId(),
                summary.getCxcNumber(),
                summary.getAbonosNumber(),
                summary.getProductionOrderId(),
                odpNumber,
                summary.getClientId(),
                summary.getTotalUnits(),
                summary.getDeliveredUnits(),
                summary.getPendingUnits(),
                summary.getTotalOwed(),
                nullToZero(summary.getTotalPaid()),
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

    private static AccountsReceivableView withoutMovements(ProductionOrder order, String odpNumber) {
        AbonosBalance.Snapshot balance = AbonosBalance.of(order, BigDecimal.ZERO);
        int requested = order.getRequestedQuantity();
        return new AccountsReceivableView(
                null,
                null,
                null,
                order.getProductionOrderId(),
                odpNumber,
                order.getClientId(),
                requested,
                0,
                requested,
                balance.totalOwed(),
                balance.totalPaid(),
                balance.totalRemaining(),
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
            String abonosNumber,
            String productionOrderId,
            String odpNumber,
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
