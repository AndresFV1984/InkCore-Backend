package com.inkcore.application.order.usecase;

import com.inkcore.application.order.AbonosBalance;
import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.model.NegotiatedAdditionApprovalChannel;
import com.inkcore.domain.order.model.NegotiatedAdditionMovementType;
import com.inkcore.domain.order.model.NegotiatedAdditionReason;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.NegotiatedAdditionRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.service.ProductionOrderCalculator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class CreateNegotiatedAdditionUseCase {

    private static final int CONCEPT_MAX = 200;
    private static final int APPROVER_MAX = 150;

    private final OrderSupport support;
    private final NegotiatedAdditionRepositoryPort additionRepository;
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;

    public CreateNegotiatedAdditionUseCase(
            OrderSupport support,
            NegotiatedAdditionRepositoryPort additionRepository,
            AccountsReceivableRepositoryPort accountsReceivableRepository
    ) {
        this.support = support;
        this.additionRepository = additionRepository;
        this.accountsReceivableRepository = accountsReceivableRepository;
    }

    @Transactional
    public NegotiatedAdditionResult execute(CreateNegotiatedAdditionCommand command, Authentication authentication) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        LocalDateTime now = support.now();

        String concept = requireText(command.concept(), "concept", CONCEPT_MAX);
        String approvedBy = requireText(command.approvedByClientName(), "approvedByClientName", APPROVER_MAX);
        NegotiatedAdditionReason reason = NegotiatedAdditionReason.fromValue(command.changeReason());
        NegotiatedAdditionApprovalChannel channel = NegotiatedAdditionApprovalChannel.fromValue(command.approvalChannel());
        int scheduleDays = command.scheduleImpactDays() == null ? 0 : command.scheduleImpactDays();
        if (scheduleDays < 0) {
            throw new OrderBusinessRuleException("scheduleImpactDays no puede ser negativo");
        }
        String reference = blankToNull(command.approvalReference());
        if (reference != null && reference.length() > 120) {
            throw new OrderBusinessRuleException("approvalReference supera 120 caracteres");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessRuleException("amount debe ser mayor que 0");
        }

        ProductionOrder order = support.requireActiveOrder(command.productionOrderId(), companyId);
        BigDecimal amount = command.amount().setScale(2, RoundingMode.HALF_UP);

        NegotiatedAddition addition = new NegotiatedAddition();
        addition.setCompanyId(companyId);
        addition.setAdditionNumber(support.nextNegotiatedAdditionNumber(companyId));
        addition.setProductionOrderId(order.getProductionOrderId());
        addition.setClientId(order.getClientId());
        addition.setMovementType(NegotiatedAdditionMovementType.REGISTRO);
        addition.setConcept(concept);
        addition.setChangeReason(reason);
        addition.setAmount(amount);
        addition.setScheduleImpactDays(scheduleDays);
        addition.setApprovedByClientName(approvedBy);
        addition.setApprovalChannel(channel);
        addition.setApprovalReference(reference);
        addition.setApprovedAt(command.approvedAt() == null ? now : command.approvedAt());
        addition.setRegisteredBy(userId);
        addition.setNotes(blankToNull(command.notes()));
        addition.setCreatedAt(now);

        NegotiatedAddition saved = additionRepository.save(addition);
        return finish(companyId, order, saved);
    }

    NegotiatedAdditionResult finish(String companyId, ProductionOrder order, NegotiatedAddition saved) {
        order.setNegotiatedAdditionsTotal(
                additionRepository.sumNetAmount(companyId, order.getProductionOrderId())
        );
        AccountsReceivable summary = accountsReceivableRepository
                .findByProductionOrderId(companyId, order.getProductionOrderId())
                .orElseGet(AccountsReceivable::new);
        AbonosBalance.applyTo(summary, order);
        String odpNumber = support.resolveOdpNumber(companyId, order.getProductionOrderId());
        return new NegotiatedAdditionResult(
                saved,
                ProductionOrderCalculator.calculateTotalToCharge(order),
                order.getNegotiatedAdditionsTotal(),
                ProductionOrderCalculator.calculateAmountToCollect(order),
                summary,
                odpNumber
        );
    }

    private static String requireText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new OrderBusinessRuleException(field + " es obligatorio");
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new OrderBusinessRuleException(field + " supera " + max + " caracteres");
        }
        return trimmed;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateNegotiatedAdditionCommand(
            String productionOrderId,
            String concept,
            String changeReason,
            BigDecimal amount,
            Integer scheduleImpactDays,
            String approvedByClientName,
            String approvalChannel,
            String approvalReference,
            LocalDateTime approvedAt,
            String notes
    ) {
    }

    public record NegotiatedAdditionResult(
            NegotiatedAddition addition,
            BigDecimal productionTotal,
            BigDecimal negotiatedAdditionsTotal,
            BigDecimal amountToCollect,
            AccountsReceivable accountsReceivable,
            String odpNumber
    ) {
    }
}
