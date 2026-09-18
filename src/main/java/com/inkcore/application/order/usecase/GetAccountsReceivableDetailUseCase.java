package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.AccountsReceivableAging;
import com.inkcore.domain.order.model.DeliveryMovementType;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderPaymentRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class GetAccountsReceivableDetailUseCase {

    private final OrderSupport support;
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;
    private final OrderDeliveryRepositoryPort deliveryRepository;
    private final OrderPaymentRepositoryPort paymentRepository;
    private final ClientRepositoryPort clientRepository;

    public GetAccountsReceivableDetailUseCase(
            OrderSupport support,
            AccountsReceivableRepositoryPort accountsReceivableRepository,
            OrderDeliveryRepositoryPort deliveryRepository,
            OrderPaymentRepositoryPort paymentRepository,
            ClientRepositoryPort clientRepository
    ) {
        this.support = support;
        this.accountsReceivableRepository = accountsReceivableRepository;
        this.deliveryRepository = deliveryRepository;
        this.paymentRepository = paymentRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public AccountsReceivableDetail execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        ProductionOrder order = support.requireActiveOrder(productionOrderId, companyId);

        AccountsReceivable summary = accountsReceivableRepository.findByProductionOrderId(companyId, productionOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNTS_RECEIVABLE_NOT_FOUND",
                        "No hay cuentas por cobrar para esta orden"
                ));

        String clientName = clientRepository.findById(summary.getClientId())
                .map(Client::getName)
                .orElse(null);

        List<OrderDelivery> deliveries = deliveryRepository.findByProductionOrderId(companyId, productionOrderId);
        String lastDeliveryNumber = deliveries.stream()
                .filter(d -> d.getMovementType() == DeliveryMovementType.ENTREGA)
                .map(OrderDelivery::getDeliveryNumber)
                .filter(n -> n != null && !n.isBlank())
                .findFirst()
                .orElse(null);

        AccountsReceivableAging.AgingSnapshot aging = AccountsReceivableAging.of(summary, LocalDate.now());
        ListAccountsReceivableUseCase.AccountsReceivableRow row = new ListAccountsReceivableUseCase.AccountsReceivableRow(
                summary.getAccountsReceivableId(),
                summary.getCxcNumber(),
                summary.getProductionOrderId(),
                order.getOrderNumber(),
                summary.getClientId(),
                clientName,
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
                summary.getOpenedAt(),
                summary.getDueDate(),
                summary.getPaymentTermDays(),
                aging.collectionStatus(),
                aging.daysOverdue(),
                aging.agingBucket(),
                aging.overdue(),
                aging.dueSoon(),
                lastDeliveryNumber,
                summary.getLastDeliveryAt(),
                summary.getLastPaymentNumber(),
                summary.getLastPaymentAt()
        );

        List<OrderPayment> payments = paymentRepository.findByProductionOrderId(companyId, productionOrderId);
        return new AccountsReceivableDetail(row, deliveries, payments);
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record AccountsReceivableDetail(
            ListAccountsReceivableUseCase.AccountsReceivableRow summary,
            List<OrderDelivery> deliveries,
            List<OrderPayment> payments
    ) {
    }
}
