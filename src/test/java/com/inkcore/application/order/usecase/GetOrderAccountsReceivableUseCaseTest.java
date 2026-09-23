package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.productionorder.model.PrepressDetails;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderAccountsReceivableUseCaseTest {

    @Mock OrderSupport support;
    @Mock AccountsReceivableRepositoryPort repository;
    @Mock Authentication authentication;

    private GetOrderAccountsReceivableUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetOrderAccountsReceivableUseCase(support, repository);
    }

    @Test
    void execute_withoutMovements_exposesTotalToChargeAsAbonosBase() {
        ProductionOrder order = orderWithCharge(new BigDecimal("1500000.00"));

        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(order);
        when(support.resolveOdpNumber("company-1", "op-1")).thenReturn(null);
        when(repository.findByProductionOrderId("company-1", "op-1")).thenReturn(Optional.empty());

        GetOrderAccountsReceivableUseCase.AccountsReceivableView result = useCase.execute("op-1", authentication);

        assertEquals("sin_movimientos", result.status());
        assertFalse(result.hasMovements());
        assertEquals(null, result.accountsReceivableId());
        assertEquals(null, result.cxcNumber());
        assertEquals(null, result.abonosNumber());
        assertEquals(1000, result.totalUnits());
        assertEquals(1000, result.pendingUnits());
        assertEquals(0, new BigDecimal("1500000.00").compareTo(result.totalOwed()));
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(result.totalPaid()));
        assertEquals(0, new BigDecimal("1500000.00").compareTo(result.totalRemaining()));
    }

    @Test
    void execute_returnsAbonosBalanceFromTotalToChargeNotDeliveryOwed() {
        ProductionOrder order = orderWithCharge(new BigDecimal("1500000.00"));

        com.inkcore.domain.order.model.AccountsReceivable summary = new com.inkcore.domain.order.model.AccountsReceivable();
        summary.setAccountsReceivableId("ar-1");
        summary.setCxcNumber("CXC-7");
        summary.setAbonosNumber("ABN-7");
        summary.setProductionOrderId("op-1");
        summary.setClientId("client-1");
        summary.setTotalUnits(1000);
        summary.setDeliveredUnits(0);
        summary.setPendingUnits(1000);
        summary.setTotalOwed(BigDecimal.ZERO);
        summary.setTotalPaid(new BigDecimal("300000.00"));
        summary.setTotalRemaining(new BigDecimal("-300000.00"));
        summary.setStatus(com.inkcore.domain.order.model.AccountsReceivableStatus.PARCIAL);
        summary.setLastPaymentNumber("ABN-4");
        summary.setLastPaymentAt(java.time.LocalDateTime.of(2026, 9, 5, 16, 0));

        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(order);
        when(support.resolveOdpNumber("company-1", "op-1")).thenReturn("ODP-12");
        when(repository.findByProductionOrderId("company-1", "op-1")).thenReturn(Optional.of(summary));

        GetOrderAccountsReceivableUseCase.AccountsReceivableView result = useCase.execute("op-1", authentication);

        assertEquals("ar-1", result.accountsReceivableId());
        assertEquals("CXC-7", result.cxcNumber());
        assertEquals("ABN-7", result.abonosNumber());
        assertEquals("ODP-12", result.odpNumber());
        assertEquals("parcial", result.status());
        assertEquals(true, result.hasMovements());
        assertEquals("ABN-4", result.lastPaymentNumber());
        assertFalse(result.abonosNumber().equals(result.lastPaymentNumber()));
        assertEquals(0, new BigDecimal("1500000.00").compareTo(result.totalOwed()));
        assertEquals(0, new BigDecimal("300000.00").compareTo(result.totalPaid()));
        assertEquals(0, new BigDecimal("1200000.00").compareTo(result.totalRemaining()));
    }

    private static ProductionOrder orderWithCharge(BigDecimal totalToCharge) {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("op-1");
        order.setCompanyId("company-1");
        order.setClientId("client-1");
        order.setRequestedQuantity(1000);
        order.setState(true);
        PrepressDetails prepress = new PrepressDetails();
        prepress.setTotalPlatesValue(totalToCharge);
        order.setPrepress(prepress);
        return order;
    }
}
