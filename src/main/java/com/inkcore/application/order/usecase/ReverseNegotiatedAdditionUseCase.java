package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.exception.OrderConflictException;
import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.ports.out.NegotiatedAdditionRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReverseNegotiatedAdditionUseCase {

    private final OrderSupport support;
    private final NegotiatedAdditionRepositoryPort additionRepository;
    private final CreateNegotiatedAdditionUseCase createUseCase;

    public ReverseNegotiatedAdditionUseCase(
            OrderSupport support,
            NegotiatedAdditionRepositoryPort additionRepository,
            CreateNegotiatedAdditionUseCase createUseCase
    ) {
        this.support = support;
        this.additionRepository = additionRepository;
        this.createUseCase = createUseCase;
    }

    @Transactional
    public CreateNegotiatedAdditionUseCase.NegotiatedAdditionResult execute(
            String productionOrderId,
            String additionId,
            String reason,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        LocalDateTime now = support.now();

        ProductionOrder order = support.requireActiveOrder(productionOrderId, companyId);
        NegotiatedAddition original = additionRepository.findByIdForUpdate(companyId, additionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NEGOTIATED_ADDITION_NOT_FOUND",
                        "Adicional negociado no encontrado"
                ));
        if (!productionOrderId.equals(original.getProductionOrderId())) {
            throw new ResourceNotFoundException(
                    "NEGOTIATED_ADDITION_NOT_FOUND",
                    "Adicional negociado no encontrado"
            );
        }
        if (!original.getMovementType().isReversibleRegistration() || original.isVoided()) {
            throw new OrderBusinessRuleException("Solo se puede anular un adicional vigente");
        }
        if (additionRepository.existsReversionFor(companyId, additionId)) {
            throw new OrderConflictException("Este adicional ya fue anulado");
        }

        String voidNote = reason == null || reason.isBlank()
                ? "Adicional anulado"
                : "Anulado por: " + reason.trim();
        String notes = original.getNotes();
        original.setNotes(notes == null || notes.isBlank() ? voidNote : notes + "\n" + voidNote);
        original.setVoidedAt(now);
        original.setVoidedBy(userId);

        NegotiatedAddition saved = additionRepository.update(original);
        return createUseCase.finish(companyId, order, saved);
    }
}
