package com.inkcore.application.order.usecase;

import com.inkcore.application.order.AbonosBalance;
import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GetClientAccountsReceivableUseCase {

    private final OrderSupport support;
    private final ClientRepositoryPort clientRepository;
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;

    public GetClientAccountsReceivableUseCase(
            OrderSupport support,
            ClientRepositoryPort clientRepository,
            AccountsReceivableRepositoryPort accountsReceivableRepository
    ) {
        this.support = support;
        this.clientRepository = clientRepository;
        this.accountsReceivableRepository = accountsReceivableRepository;
    }

    @Transactional(readOnly = true)
    public ClientAccountsReceivable execute(String clientId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        Client client = clientRepository.findById(clientId)
                .filter(found -> companyId.equals(found.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLIENT_NOT_FOUND",
                        "Cliente no encontrado"
                ));

        List<OrderAccountsReceivable> orders = accountsReceivableRepository.findByClientId(companyId, clientId)
                .stream()
                .map(this::toOrderSummary)
                .toList();

        BigDecimal totalOwed = orders.stream()
                .map(OrderAccountsReceivable::totalOwed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = orders.stream()
                .map(OrderAccountsReceivable::totalPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = orders.stream()
                .map(OrderAccountsReceivable::totalRemaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClientAccountsReceivable(
                client.getClientId(),
                client.getName(),
                orders,
                totalOwed,
                totalPaid,
                totalRemaining
        );
    }

    private OrderAccountsReceivable toOrderSummary(AccountsReceivable summary) {
        ProductionOrder order = support.productionOrderRepository()
                .findById(summary.getProductionOrderId())
                .orElse(null);
        String orderNumber = order == null ? null : order.getOrderNumber();
        AbonosBalance.applyTo(summary, order);
        return new OrderAccountsReceivable(
                summary.getAccountsReceivableId(),
                summary.getCxcNumber(),
                summary.getAbonosNumber(),
                summary.getProductionOrderId(),
                orderNumber,
                summary.getTotalUnits(),
                summary.getDeliveredUnits(),
                summary.getPendingUnits(),
                summary.getTotalOwed(),
                nullToZero(summary.getTotalPaid()),
                summary.getTotalRemaining(),
                summary.getStatus() == null ? null : summary.getStatus().getDbValue()
        );
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record ClientAccountsReceivable(
            String clientId,
            String clientName,
            List<OrderAccountsReceivable> orders,
            BigDecimal totalOwed,
            BigDecimal totalPaid,
            BigDecimal totalRemaining
    ) {
    }

    public record OrderAccountsReceivable(
            String accountsReceivableId,
            String cxcNumber,
            String abonosNumber,
            String productionOrderId,
            String orderNumber,
            int totalUnits,
            int deliveredUnits,
            int pendingUnits,
            BigDecimal totalOwed,
            BigDecimal totalPaid,
            BigDecimal totalRemaining,
            String status
    ) {
    }
}
