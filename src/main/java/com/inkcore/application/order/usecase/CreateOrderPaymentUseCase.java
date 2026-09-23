package com.inkcore.application.order.usecase;

import com.inkcore.application.order.AbonosBalance;
import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.PaymentMethod;
import com.inkcore.domain.order.model.PaymentType;
import com.inkcore.domain.order.model.WithholdingType;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
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
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;

    public CreateOrderPaymentUseCase(
            OrderSupport support,
            OrderPaymentRepositoryPort paymentRepository,
            AccountsReceivableRepositoryPort accountsReceivableRepository
    ) {
        this.support = support;
        this.paymentRepository = paymentRepository;
        this.accountsReceivableRepository = accountsReceivableRepository;
    }

    @Transactional
    public CreatePaymentResult execute(CreateOrderPaymentCommand command, Authentication authentication) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        LocalDateTime now = support.now();

        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessRuleException("amount debe ser mayor que 0");
        }

        PaymentType paymentType = resolvePaymentType(command.paymentType());
        if (paymentType == PaymentType.REVERSION) {
            throw new OrderBusinessRuleException("Use el endpoint de reversión para anular un movimiento");
        }

        PaymentMethod method = resolvePaymentMethod(paymentType, command.paymentMethod());
        WithholdingType withholdingType = WithholdingType.fromValue(command.withholdingType());
        validateSettlement(paymentType, method, withholdingType);

        ProductionOrder order = support.requireActiveOrder(command.productionOrderId(), companyId);
        BigDecimal amount = command.amount().setScale(2, RoundingMode.HALF_UP);

        OrderPayment payment = new OrderPayment();
        payment.setCompanyId(companyId);
        payment.setPaymentNumber(support.nextPaymentNumber(companyId));
        payment.setProductionOrderId(order.getProductionOrderId());
        payment.setClientId(order.getClientId());
        payment.setPaymentType(paymentType);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setReference(blankToNull(command.reference()));
        payment.setWithholdingType(withholdingType);
        payment.setWithholdingBase(scaleNullable(command.withholdingBase()));
        payment.setWithholdingRate(scaleRate(command.withholdingRate()));
        payment.setCertificateRef(blankToNull(command.certificateRef()));
        payment.setInvoiceId(blankToNull(command.invoiceId()));
        payment.setPaidAt(command.paidAt() == null ? now : command.paidAt());
        payment.setRegisteredBy(userId);
        payment.setNotes(command.notes());
        payment.setCreatedAt(now);

        OrderPayment saved = paymentRepository.save(payment);
        AccountsReceivable summary = accountsReceivableRepository
                .findByProductionOrderId(companyId, order.getProductionOrderId())
                .orElseGet(AccountsReceivable::new);
        AbonosBalance.applyTo(summary, order);
        String odpNumber = support.resolveOdpNumber(companyId, order.getProductionOrderId());
        return new CreatePaymentResult(saved, summary, odpNumber);
    }

    private static PaymentType resolvePaymentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return PaymentType.ABONO;
        }
        return PaymentType.fromValue(raw);
    }

    private static PaymentMethod resolvePaymentMethod(PaymentType type, String rawMethod) {
        if (type == PaymentType.RETENCION) {
            if (rawMethod == null || rawMethod.isBlank() || "retencion".equalsIgnoreCase(rawMethod.trim())) {
                return PaymentMethod.RETENCION;
            }
            throw new OrderBusinessRuleException("paymentMethod debe ser retencion cuando paymentType=retencion");
        }
        return PaymentMethod.fromValue(rawMethod);
    }

    private static void validateSettlement(
            PaymentType type,
            PaymentMethod method,
            WithholdingType withholdingType
    ) {
        if (type == PaymentType.RETENCION) {
            if (withholdingType == null) {
                throw new OrderBusinessRuleException("withholdingType es obligatorio para retencion");
            }
            if (method != PaymentMethod.RETENCION) {
                throw new OrderBusinessRuleException("paymentMethod debe ser retencion");
            }
            return;
        }
        if (!method.isCashChannel()) {
            throw new OrderBusinessRuleException("paymentMethod inválido para " + type.getDbValue());
        }
        if (withholdingType != null) {
            throw new OrderBusinessRuleException("withholdingType solo aplica a paymentType=retencion");
        }
    }

    private static BigDecimal scaleNullable(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal scaleRate(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateOrderPaymentCommand(
            String productionOrderId,
            BigDecimal amount,
            String paymentType,
            String paymentMethod,
            String reference,
            String withholdingType,
            BigDecimal withholdingBase,
            BigDecimal withholdingRate,
            String certificateRef,
            String invoiceId,
            LocalDateTime paidAt,
            String notes
    ) {
    }

    public record CreatePaymentResult(
            OrderPayment payment,
            AccountsReceivable accountsReceivable,
            String odpNumber
    ) {
    }
}
