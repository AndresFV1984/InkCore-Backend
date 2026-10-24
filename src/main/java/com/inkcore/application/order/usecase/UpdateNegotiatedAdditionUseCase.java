package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.exception.OrderConflictException;
import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.model.NegotiatedAdditionApprovalChannel;
import com.inkcore.domain.order.model.NegotiatedAdditionReason;
import com.inkcore.domain.order.ports.out.NegotiatedAdditionRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class UpdateNegotiatedAdditionUseCase {

    private static final int CONCEPT_MAX = 200;
    private static final int APPROVER_MAX = 150;

    private final OrderSupport support;
    private final NegotiatedAdditionRepositoryPort additionRepository;
    private final CreateNegotiatedAdditionUseCase createUseCase;
    private final OrderCxcDocumentService cxcDocumentService;

    public UpdateNegotiatedAdditionUseCase(
            OrderSupport support,
            NegotiatedAdditionRepositoryPort additionRepository,
            CreateNegotiatedAdditionUseCase createUseCase,
            OrderCxcDocumentService cxcDocumentService
    ) {
        this.support = support;
        this.additionRepository = additionRepository;
        this.createUseCase = createUseCase;
        this.cxcDocumentService = cxcDocumentService;
    }

    @Transactional
    public CreateNegotiatedAdditionUseCase.NegotiatedAdditionResult execute(
            String productionOrderId,
            String additionId,
            CreateNegotiatedAdditionUseCase.CreateNegotiatedAdditionCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        LocalDateTime now = support.now();
        ProductionOrder order = support.requireActiveOrder(productionOrderId, companyId);

        NegotiatedAddition addition = additionRepository.findByIdForUpdate(companyId, additionId)
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
        if (!addition.getMovementType().isReversibleRegistration()) {
            throw new OrderBusinessRuleException("Solo se puede editar un adicional registrado");
        }
        if (additionRepository.existsReversionFor(companyId, additionId)) {
            throw new OrderConflictException("Un adicional anulado no se puede editar");
        }

        String concept = requireText(command.concept(), "concept", CONCEPT_MAX);
        String approvedBy = requireText(command.approvedByClientName(), "approvedByClientName", APPROVER_MAX);
        int scheduleDays = command.scheduleImpactDays() == null ? 0 : command.scheduleImpactDays();
        if (scheduleDays < 0) {
            throw new OrderBusinessRuleException("scheduleImpactDays no puede ser negativo");
        }
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessRuleException("amount debe ser mayor que 0");
        }
        String reference = blankToNull(command.approvalReference());
        if (reference != null && reference.length() > 120) {
            throw new OrderBusinessRuleException("approvalReference supera 120 caracteres");
        }

        addition.setConcept(concept);
        addition.setChangeReason(NegotiatedAdditionReason.fromValue(command.changeReason()));
        addition.setAmount(command.amount().setScale(2, RoundingMode.HALF_UP));
        addition.setScheduleImpactDays(scheduleDays);
        addition.setApprovedByClientName(approvedBy);
        addition.setApprovalChannel(NegotiatedAdditionApprovalChannel.fromValue(command.approvalChannel()));
        addition.setApprovalReference(reference);
        addition.setApprovedAt(command.approvedAt() == null ? now : command.approvedAt());
        addition.setNotes(blankToNull(command.notes()));

        NegotiatedAddition saved = additionRepository.update(addition);
        cxcDocumentService.syncTotalFromAddition(saved, now);
        return createUseCase.finish(companyId, order, saved);
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
}
