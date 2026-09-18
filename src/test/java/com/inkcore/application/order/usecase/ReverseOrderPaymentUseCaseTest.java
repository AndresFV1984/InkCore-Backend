package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.PaymentMethod;
import com.inkcore.domain.order.model.PaymentType;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderPaymentRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReverseOrderPaymentUseCaseTest {

    @Mock OrderSupport support;
    @Mock OrderPaymentRepositoryPort paymentRepository;
    @Mock AccountsReceivableRepositoryPort accountsReceivableRepository;
    @Mock Authentication authentication;

    private ReverseOrderPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReverseOrderPaymentUseCase(support, paymentRepository, accountsReceivableRepository);
    }

    @Test
    void execute_locksOriginalAndAppendsReversion() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 12, 0);
        OrderPayment original = originalPayment();
        AccountsReceivable updated = new AccountsReceivable();
        updated.setProductionOrderId("op-1");
        updated.setLastPaymentNumber("ABN-1");
        updated.setLastPaymentAt(LocalDateTime.of(2026, 9, 5, 16, 0));
        updated.setTotalPaid(new BigDecimal("100000.00"));
        updated.setTotalRemaining(new BigDecimal("500000.00"));

        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-authenticated");
        when(support.now()).thenReturn(now);
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(ProductionOrder.reconstitute());
        when(support.nextPaymentNumber("company-1")).thenReturn("ABN-9");
        when(paymentRepository.findByIdForUpdate("company-1", "payment-1"))
                .thenReturn(Optional.of(original));
        when(paymentRepository.existsReversionFor("company-1", "payment-1")).thenReturn(false);
        when(paymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountsReceivableRepository.findByProductionOrderId("company-1", "op-1"))
                .thenReturn(Optional.of(updated));

        var result = useCase.execute("op-1", "payment-1", "Comprobante duplicado", authentication);

        verify(paymentRepository).findByIdForUpdate("company-1", "payment-1");
        ArgumentCaptor<OrderPayment> captor = ArgumentCaptor.forClass(OrderPayment.class);
        verify(paymentRepository).save(captor.capture());
        OrderPayment reversion = captor.getValue();
        assertEquals(PaymentType.REVERSION, reversion.getPaymentType());
        assertEquals("ABN-9", reversion.getPaymentNumber());
        assertEquals("payment-1", reversion.getReversedPaymentId());
        assertEquals(original.getAmount(), reversion.getAmount());
        assertEquals("user-authenticated", reversion.getRegisteredBy());
        assertEquals("Anulado por: Comprobante duplicado", reversion.getNotes());
        assertEquals("ABN-1", result.accountsReceivable().getLastPaymentNumber());
        assertEquals(0, new BigDecimal("100000.00").compareTo(result.accountsReceivable().getTotalPaid()));
    }

    private static OrderPayment originalPayment() {
        OrderPayment payment = new OrderPayment();
        payment.setOrderPaymentId("payment-1");
        payment.setCompanyId("company-1");
        payment.setProductionOrderId("op-1");
        payment.setClientId("client-1");
        payment.setPaymentType(PaymentType.ABONO);
        payment.setAmount(new BigDecimal("100000.00"));
        payment.setPaymentMethod(PaymentMethod.TRANSFERENCIA);
        return payment;
    }
}
