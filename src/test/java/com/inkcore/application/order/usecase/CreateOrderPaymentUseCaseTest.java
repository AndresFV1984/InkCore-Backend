package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.PaymentType;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderPaymentRepositoryPort;
import com.inkcore.domain.productionorder.model.PrepressDetails;
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
class CreateOrderPaymentUseCaseTest {

    @Mock OrderSupport support;
    @Mock OrderPaymentRepositoryPort paymentRepository;
    @Mock AccountsReceivableRepositoryPort accountsReceivableRepository;
    @Mock Authentication authentication;

    private CreateOrderPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateOrderPaymentUseCase(support, paymentRepository, accountsReceivableRepository);
    }

    @Test
    void execute_allowsAdvanceWithoutDeliveriesOrTotalToCharge() {
        ProductionOrder order = bareOrder();
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 10, 0);
        AccountsReceivable updated = summary(new BigDecimal("-250000.00"));
        updated.setLastPaymentNumber("ABN-1");
        updated.setLastPaymentAt(now);
        updated.setTotalPaid(new BigDecimal("250000.00"));
        updated.setCxcNumber("CXC-1");
        updated.setDeliveredUnits(0);

        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-authenticated");
        when(support.now()).thenReturn(now);
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(order);
        when(support.nextPaymentNumber("company-1")).thenReturn("ABN-1");
        when(paymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountsReceivableRepository.findByProductionOrderId("company-1", "op-1"))
                .thenReturn(Optional.of(updated));
        when(support.resolveOdpNumber("company-1", "op-1")).thenReturn("ODP-12");

        var result = useCase.execute(
                new CreateOrderPaymentUseCase.CreateOrderPaymentCommand(
                        "op-1",
                        new BigDecimal("250000"),
                        "anticipo",
                        "transferencia",
                        "REF-1",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Anticipo"
                ),
                authentication
        );

        ArgumentCaptor<OrderPayment> captor = ArgumentCaptor.forClass(OrderPayment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals(PaymentType.ANTICIPO, captor.getValue().getPaymentType());
        assertEquals("ABN-1", captor.getValue().getPaymentNumber());
        assertEquals("user-authenticated", captor.getValue().getRegisteredBy());
        assertEquals(0, new BigDecimal("250000.00").compareTo(captor.getValue().getAmount()));
        assertEquals("ODP-12", result.odpNumber());
        assertEquals("ABN-1", result.accountsReceivable().getLastPaymentNumber());
        assertEquals(0, new BigDecimal("250000.00").compareTo(result.accountsReceivable().getTotalPaid()));
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(result.accountsReceivable().getTotalOwed()));
        assertEquals(0, new BigDecimal("-250000.00").compareTo(result.accountsReceivable().getTotalRemaining()));
    }

    @Test
    void execute_abonoAgainstOpTotalEvenWithZeroDeliveries() {
        ProductionOrder order = orderWithCharge(new BigDecimal("1500000.00"));
        AccountsReceivable updated = summary(new BigDecimal("-300000.00"));
        updated.setCxcNumber("CXC-7");
        updated.setAbonosNumber("ABN-7");
        updated.setDeliveredUnits(0);
        updated.setLastPaymentNumber("ABN-2");
        updated.setLastPaymentAt(LocalDateTime.of(2026, 9, 15, 16, 0));
        updated.setTotalPaid(new BigDecimal("300000.00"));
        updated.setTotalOwed(BigDecimal.ZERO);

        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-authenticated");
        when(support.now()).thenReturn(LocalDateTime.of(2026, 9, 15, 16, 0));
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(order);
        when(support.nextPaymentNumber("company-1")).thenReturn("ABN-2");
        when(paymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountsReceivableRepository.findByProductionOrderId("company-1", "op-1"))
                .thenReturn(Optional.of(updated));

        var result = useCase.execute(
                new CreateOrderPaymentUseCase.CreateOrderPaymentCommand(
                        "op-1",
                        new BigDecimal("300000"),
                        "abono",
                        "transferencia",
                        "REF-2",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Abono sobre total OP"
                ),
                authentication
        );

        assertEquals("ABN-2", result.payment().getPaymentNumber());
        assertEquals("ABN-2", result.accountsReceivable().getLastPaymentNumber());
        assertEquals(0, new BigDecimal("1500000.00").compareTo(result.accountsReceivable().getTotalOwed()));
        assertEquals(0, new BigDecimal("300000.00").compareTo(result.accountsReceivable().getTotalPaid()));
        assertEquals(0, new BigDecimal("1200000.00").compareTo(result.accountsReceivable().getTotalRemaining()));
    }

    @Test
    void execute_registersWithholding() {
        ProductionOrder order = bareOrder();
        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-authenticated");
        when(support.now()).thenReturn(LocalDateTime.of(2026, 9, 8, 10, 0));
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(order);
        when(support.nextPaymentNumber("company-1")).thenReturn("ABN-2");
        when(paymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountsReceivableRepository.findByProductionOrderId("company-1", "op-1"))
                .thenReturn(Optional.of(summary(BigDecimal.ZERO)));

        useCase.execute(
                new CreateOrderPaymentUseCase.CreateOrderPaymentCommand(
                        "op-1",
                        new BigDecimal("50000"),
                        "retencion",
                        "retencion",
                        null,
                        "retefuente",
                        new BigDecimal("1000000"),
                        new BigDecimal("2.5"),
                        "CERT-1",
                        null,
                        null,
                        null
                ),
                authentication
        );

        ArgumentCaptor<OrderPayment> captor = ArgumentCaptor.forClass(OrderPayment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals(PaymentType.RETENCION, captor.getValue().getPaymentType());
        assertEquals("retefuente", captor.getValue().getWithholdingType().getDbValue());
    }

    private static ProductionOrder bareOrder() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("op-1");
        order.setCompanyId("company-1");
        order.setClientId("client-1");
        order.setState(true);
        return order;
    }

    private static ProductionOrder orderWithCharge(BigDecimal totalToCharge) {
        ProductionOrder order = bareOrder();
        PrepressDetails prepress = new PrepressDetails();
        prepress.setTotalPlatesValue(totalToCharge);
        order.setPrepress(prepress);
        return order;
    }

    private static AccountsReceivable summary(BigDecimal remaining) {
        AccountsReceivable summary = new AccountsReceivable();
        summary.setProductionOrderId("op-1");
        summary.setCompanyId("company-1");
        summary.setClientId("client-1");
        summary.setTotalRemaining(remaining);
        return summary;
    }
}
