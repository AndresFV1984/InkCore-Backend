package com.inkcore.application.order.usecase;

import com.inkcore.application.order.AbonosBalance;
import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.model.OrderCxcDocument;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.NegotiatedAdditionRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CreateCxcFromAdditionUseCase {

    private final OrderSupport support;
    private final NegotiatedAdditionRepositoryPort additionRepository;
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;
    private final OrderCxcDocumentService cxcDocumentService;

    public CreateCxcFromAdditionUseCase(
            OrderSupport support,
            NegotiatedAdditionRepositoryPort additionRepository,
            AccountsReceivableRepositoryPort accountsReceivableRepository,
            OrderCxcDocumentService cxcDocumentService
    ) {
        this.support = support;
        this.additionRepository = additionRepository;
        this.accountsReceivableRepository = accountsReceivableRepository;
        this.cxcDocumentService = cxcDocumentService;
    }

    @Transactional
    public Result execute(String productionOrderId, String additionId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        LocalDateTime now = support.now();

        if (additionId == null || additionId.isBlank()) {
            throw new OrderBusinessRuleException("additionId es obligatorio");
        }

        ProductionOrder order = support.requireActiveOrder(productionOrderId, companyId);
        NegotiatedAddition addition = additionRepository
                .findByIdForUpdate(companyId, additionId.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NEGOTIATED_ADDITION_NOT_FOUND",
                        "Adicional negociado no encontrado"
                ));
        if (!productionOrderId.equals(addition.getProductionOrderId())) {
            throw new ResourceNotFoundException(
                    "NEGOTIATED_ADDITION_NOT_FOUND",
                    "Adicional negociado no encontrado"
            );
        }

        AccountsReceivable summary = accountsReceivableRepository
                .findByProductionOrderIdFresh(companyId, productionOrderId)
                .orElseGet(AccountsReceivable::new);
        AbonosBalance.applyTo(summary, order);

        CustomerOrder customerOrder = support.customerOrderRepository()
                .findByProductionOrderId(companyId, productionOrderId)
                .orElse(null);

        OrderCxcDocument cxc = cxcDocumentService.createFromAddition(
                addition, summary, customerOrder, now);
        String odpNumber = support.resolveOdpNumber(companyId, productionOrderId);
        return new Result(cxc, summary, odpNumber);
    }

    public record Result(
            OrderCxcDocument cxcDocument,
            AccountsReceivable accountsReceivable,
            String odpNumber
    ) {
    }
}
