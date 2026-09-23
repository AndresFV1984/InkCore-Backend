package com.inkcore.application.order.usecase;

import com.inkcore.application.order.AbonosBalance;
import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.exception.OrderConflictException;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.PaymentType;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderPaymentRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReverseOrderPaymentUseCase {

    private final OrderSupport support;
    private final OrderPaymentRepositoryPort paymentRepository;
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;

    public ReverseOrderPaymentUseCase(
            OrderSupport support,
            OrderPaymentRepositoryPort paymentRepository,
            AccountsReceivableRepositoryPort accountsReceivableRepository
    ) {
        this.support = support;
        this.paymentRepository = paymentRepository;
        this.accountsReceivableRepository = accountsReceivableRepository;
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

        ProductionOrder order = support.requireActiveOrder(productionOrderId, companyId);
        OrderPayment original = paymentRepository.findByIdForUpdate(companyId, paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ORDER_PAYMENT_NOT_FOUND",
                        "Movimiento de pago no encontrado"
                ));

        if (!productionOrderId.equals(original.getProductionOrderId())) {
            throw new ResourceNotFoundException("ORDER_PAYMENT_NOT_FOUND", "Movimiento de pago no encontrado");
        }
        if (!original.getPaymentType().isReversibleSettlement()) {
            throw new OrderBusinessRuleException("Solo se pueden anular abonos, anticipos o retenciones");
        }
        if (paymentRepository.existsReversionFor(companyId, paymentId)) {
            throw new OrderConflictException("Este movimiento ya fue anulado");
        }

        OrderPayment reversion = new OrderPayment();
        reversion.setCompanyId(companyId);
        reversion.setPaymentNumber(support.nextPaymentNumber(companyId));
        reversion.setProductionOrderId(original.getProductionOrderId());
        reversion.setClientId(original.getClientId());
        reversion.setPaymentType(PaymentType.REVERSION);
        reversion.setAmount(original.getAmount());
        reversion.setPaymentMethod(original.getPaymentMethod());
        reversion.setReference(original.getReference());
        reversion.setReversedPaymentId(original.getOrderPaymentId());
        reversion.setWithholdingType(original.getWithholdingType());
        reversion.setWithholdingBase(original.getWithholdingBase());
        reversion.setWithholdingRate(original.getWithholdingRate());
        reversion.setCertificateRef(original.getCertificateRef());
        reversion.setInvoiceId(original.getInvoiceId());
        reversion.setPaidAt(now);
        reversion.setRegisteredBy(userId);
        reversion.setNotes(reason == null || reason.isBlank()
                ? "Movimiento anulado (" + original.getPaymentType().getDbValue() + ")"
                : "Anulado por: " + reason.trim());
        reversion.setCreatedAt(now);

        OrderPayment saved = paymentRepository.save(reversion);
        AccountsReceivable summary = accountsReceivableRepository
                .findByProductionOrderId(companyId, productionOrderId)
                .orElseGet(AccountsReceivable::new);
        AbonosBalance.applyTo(summary, order);
        String odpNumber = support.resolveOdpNumber(companyId, productionOrderId);
        return new CreateOrderPaymentUseCase.CreatePaymentResult(saved, summary, odpNumber);
    }
}
