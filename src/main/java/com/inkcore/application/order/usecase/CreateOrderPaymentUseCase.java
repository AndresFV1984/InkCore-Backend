package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.exception.OrderConflictException;
import com.inkcore.domain.order.model.ArSummary;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.PaymentMethod;
import com.inkcore.domain.order.model.PaymentType;
import com.inkcore.domain.order.ports.out.ArSummaryRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderPaymentRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class CreateOrderPaymentUseCase {

    private final OrderSupport support;
    private final OrderPaymentRepositoryPort paymentRepository;
    private final ArSummaryRepositoryPort arSummaryRepository;

    public CreateOrderPaymentUseCase(
            OrderSupport support,
            OrderPaymentRepositoryPort paymentRepository,
            ArSummaryRepositoryPort arSummaryRepository
    ) {
        this.support = support;
        this.paymentRepository = paymentRepository;
        this.arSummaryRepository = arSummaryRepository;
    }

    @Transactional
    public CreatePaymentResult execute(CreateOrderPaymentCommand command, Authentication authentication) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        LocalDateTime now = support.now();

        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessRuleException("amount debe ser mayor que 0");
        }
        PaymentMethod method = PaymentMethod.fromValue(command.paymentMethod());
        ProductionOrder order = support.requireActiveOrder(command.productionOrderId(), companyId);

        ArSummary current = arSummaryRepository
                .findByProductionOrderId(companyId, order.getProductionOrderId())
                .orElse(null);
        BigDecimal remaining = current == null || current.getTotalRemaining() == null
                ? BigDecimal.ZERO
                : current.getTotalRemaining();
        BigDecimal amount = command.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(remaining) > 0) {
            throw new OrderConflictException("El abono supera el saldo pendiente");
        }

        OrderPayment payment = new OrderPayment();
        payment.setCompanyId(companyId);
        payment.setProductionOrderId(order.getProductionOrderId());
        payment.setClientId(order.getClientId());
        payment.setPaymentType(PaymentType.ABONO);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setReference(blankToNull(command.reference()));
        payment.setPaidAt(command.paidAt() == null ? now : command.paidAt());
        payment.setRegisteredBy(userId);
        payment.setNotes(command.notes());
        payment.setCreatedAt(now);

        OrderPayment saved = paymentRepository.save(payment);
        ArSummary summary = arSummaryRepository
                .findByProductionOrderId(companyId, order.getProductionOrderId())
                .orElseGet(ArSummary::new);
        return new CreatePaymentResult(saved, summary);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateOrderPaymentCommand(
            String productionOrderId,
            BigDecimal amount,
            String paymentMethod,
            String reference,
            LocalDateTime paidAt,
            String notes
    ) {
    }

    public record CreatePaymentResult(OrderPayment payment, ArSummary accountsReceivable) {
    }
}
