package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.exception.OrderConflictException;
import com.inkcore.domain.order.model.ArSummary;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.PaymentType;
import com.inkcore.domain.order.ports.out.ArSummaryRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderPaymentRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReverseOrderPaymentUseCase {

    private final OrderSupport support;
    private final OrderPaymentRepositoryPort paymentRepository;
    private final ArSummaryRepositoryPort arSummaryRepository;

    public ReverseOrderPaymentUseCase(
            OrderSupport support,
            OrderPaymentRepositoryPort paymentRepository,
            ArSummaryRepositoryPort arSummaryRepository
    ) {
        this.support = support;
        this.paymentRepository = paymentRepository;
        this.arSummaryRepository = arSummaryRepository;
    }

    @Transactional
    public CreateOrderPaymentUseCase.CreatePaymentResult execute(
            String productionOrderId,
            String paymentId,
            String reason,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        LocalDateTime now = support.now();

        if (reason == null || reason.isBlank()) {
            throw new OrderBusinessRuleException("El motivo de anulación es obligatorio");
        }

        support.requireActiveOrder(productionOrderId, companyId);
        OrderPayment original = paymentRepository.findById(companyId, paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ORDER_PAYMENT_NOT_FOUND",
                        "Abono no encontrado"
                ));

        if (!productionOrderId.equals(original.getProductionOrderId())) {
            throw new ResourceNotFoundException("ORDER_PAYMENT_NOT_FOUND", "Abono no encontrado");
        }
        if (original.getPaymentType() != PaymentType.ABONO) {
            throw new OrderBusinessRuleException("Solo se pueden anular abonos (no reversiones)");
        }
        if (paymentRepository.existsReversionFor(companyId, paymentId)) {
            throw new OrderConflictException("Este abono ya fue anulado");
        }

        OrderPayment reversion = new OrderPayment();
        reversion.setCompanyId(companyId);
        reversion.setProductionOrderId(original.getProductionOrderId());
        reversion.setClientId(original.getClientId());
        reversion.setPaymentType(PaymentType.REVERSION);
        reversion.setAmount(original.getAmount());
        reversion.setPaymentMethod(original.getPaymentMethod());
        reversion.setReference(original.getReference());
        reversion.setReversedPaymentId(original.getOrderPaymentId());
        reversion.setPaidAt(now);
        reversion.setRegisteredBy(userId);
        reversion.setNotes("Anulado por: " + reason.trim());
        reversion.setCreatedAt(now);

        OrderPayment saved = paymentRepository.save(reversion);
        ArSummary summary = arSummaryRepository
                .findByProductionOrderId(companyId, productionOrderId)
                .orElseGet(ArSummary::new);
        return new CreateOrderPaymentUseCase.CreatePaymentResult(saved, summary);
    }
}
