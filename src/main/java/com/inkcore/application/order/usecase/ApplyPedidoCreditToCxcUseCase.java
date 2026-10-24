package com.inkcore.application.order.usecase;

import com.inkcore.application.order.AbonosBalance;
import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.model.OrderPaymentApplication;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplyPedidoCreditToCxcUseCase {

    private final OrderSupport support;
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;
    private final OrderCxcDocumentService cxcDocumentService;

    public ApplyPedidoCreditToCxcUseCase(
            OrderSupport support,
            AccountsReceivableRepositoryPort accountsReceivableRepository,
            OrderCxcDocumentService cxcDocumentService
    ) {
        this.support = support;
        this.accountsReceivableRepository = accountsReceivableRepository;
        this.cxcDocumentService = cxcDocumentService;
    }

    @Transactional
    public Result execute(Command command, Authentication authentication) {
        String companyId = support.companyId(authentication);
        LocalDateTime now = support.now();

        if (command.cxcDocumentId() == null || command.cxcDocumentId().isBlank()) {
            throw new OrderBusinessRuleException("cxcDocumentId es obligatorio");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessRuleException("amount debe ser mayor que 0");
        }

        ProductionOrder order = support.requireActiveOrder(command.productionOrderId(), companyId);
        String customerOrderId = support.customerOrderRepository()
                .findByProductionOrderId(companyId, command.productionOrderId())
                .map(CustomerOrder::getCustomerOrderId)
                .orElse(null);

        List<OrderPaymentApplication> applications = cxcDocumentService.applyFavorToCxc(
                companyId,
                command.productionOrderId(),
                command.cxcDocumentId().trim(),
                command.amount(),
                customerOrderId,
                now
        );

        AccountsReceivable summary = accountsReceivableRepository
                .findByProductionOrderIdFresh(companyId, command.productionOrderId())
                .orElseGet(AccountsReceivable::new);
        AbonosBalance.applyTo(summary, order);
        String odpNumber = support.resolveOdpNumber(companyId, command.productionOrderId());
        return new Result(applications, summary, odpNumber);
    }

    public record Command(String productionOrderId, String cxcDocumentId, BigDecimal amount) {
    }

    public record Result(
            List<OrderPaymentApplication> applications,
            AccountsReceivable accountsReceivable,
            String odpNumber
    ) {
    }
}
