package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.exception.OrderConflictException;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.DeliveryMovementType;
import com.inkcore.domain.order.model.DeliveryType;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReverseOrderDeliveryUseCaseTest {

    @Mock OrderSupport support;
    @Mock OrderDeliveryRepositoryPort deliveryRepository;
    @Mock AccountsReceivableRepositoryPort accountsReceivableRepository;
    @Mock Authentication authentication;

    private ReverseOrderDeliveryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReverseOrderDeliveryUseCase(support, deliveryRepository, accountsReceivableRepository);
    }

    @Test
    void execute_appendsReversionWithNewOdpAndKeepsCxc() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 12, 0);
        OrderDelivery original = originalDelivery();
        AccountsReceivable current = ar("ar-1", "CXC-7", new BigDecimal("600000.00"), BigDecimal.ZERO);
        AccountsReceivable updated = ar("ar-1", "CXC-7", BigDecimal.ZERO, BigDecimal.ZERO);

        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-authenticated");
        when(support.now()).thenReturn(now);
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(ProductionOrder.reconstitute());
        when(support.nextDeliveryNumber("company-1")).thenReturn("ODP-99");
        when(deliveryRepository.findByIdForUpdate("company-1", "del-1")).thenReturn(Optional.of(original));
        when(deliveryRepository.existsReversionFor("company-1", "del-1")).thenReturn(false);
        when(accountsReceivableRepository.findByProductionOrderId("company-1", "op-1"))
                .thenReturn(Optional.of(current), Optional.of(updated));
        when(deliveryRepository.save(any(OrderDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderDeliveryUseCase.CreateDeliveryResult result =
                useCase.execute("op-1", "del-1", "Entrega duplicada", authentication);

        ArgumentCaptor<OrderDelivery> captor = ArgumentCaptor.forClass(OrderDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        OrderDelivery reversion = captor.getValue();
        assertEquals(DeliveryMovementType.REVERSION, reversion.getMovementType());
        assertEquals("ODP-99", reversion.getDeliveryNumber());
        assertEquals("del-1", reversion.getReversedDeliveryId());
        assertEquals(original.getTotalValue(), reversion.getTotalValue());
        assertEquals("Anulado por: Entrega duplicada", reversion.getNotes());
        assertEquals("CXC-7", result.accountsReceivable().getCxcNumber());
        assertEquals("ar-1", result.accountsReceivable().getAccountsReceivableId());
    }

    @Test
    void execute_rejectsWhenAlreadyReversed() {
        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-authenticated");
        when(support.now()).thenReturn(LocalDateTime.of(2026, 9, 8, 12, 0));
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(ProductionOrder.reconstitute());
        when(deliveryRepository.findByIdForUpdate("company-1", "del-1")).thenReturn(Optional.of(originalDelivery()));
        when(deliveryRepository.existsReversionFor("company-1", "del-1")).thenReturn(true);

        assertThrows(
                OrderConflictException.class,
                () -> useCase.execute("op-1", "del-1", null, authentication)
        );
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void execute_rejectsWhenOwedWouldFallBelowPaid() {
        AccountsReceivable current = ar("ar-1", "CXC-7", new BigDecimal("600000.00"), new BigDecimal("200000.00"));

        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-authenticated");
        when(support.now()).thenReturn(LocalDateTime.of(2026, 9, 8, 12, 0));
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(ProductionOrder.reconstitute());
        when(deliveryRepository.findByIdForUpdate("company-1", "del-1")).thenReturn(Optional.of(originalDelivery()));
        when(deliveryRepository.existsReversionFor("company-1", "del-1")).thenReturn(false);
        when(accountsReceivableRepository.findByProductionOrderId("company-1", "op-1")).thenReturn(Optional.of(current));

        assertThrows(
                OrderBusinessRuleException.class,
                () -> useCase.execute("op-1", "del-1", "No aplica", authentication)
        );
        verify(deliveryRepository, never()).save(any());
    }

    private static OrderDelivery originalDelivery() {
        OrderDelivery delivery = new OrderDelivery();
        delivery.setOrderDeliveryId("del-1");
        delivery.setCompanyId("company-1");
        delivery.setDeliveryNumber("ODP-1");
        delivery.setProductionOrderId("op-1");
        delivery.setClientId("client-1");
        delivery.setSellerId("seller-1");
        delivery.setMovementType(DeliveryMovementType.ENTREGA);
        delivery.setDeliveryType(DeliveryType.PARCIAL);
        delivery.setQuantityDelivered(500);
        delivery.setUnitPrice(new BigDecimal("1200.00"));
        delivery.setTotalValue(new BigDecimal("600000.00"));
        delivery.setWorkNameSnapshot("Trabajo");
        delivery.setClientNameSnapshot("Cliente");
        return delivery;
    }

    private static AccountsReceivable ar(String id, String cxc, BigDecimal owed, BigDecimal paid) {
        AccountsReceivable summary = new AccountsReceivable();
        summary.setAccountsReceivableId(id);
        summary.setCxcNumber(cxc);
        summary.setProductionOrderId("op-1");
        summary.setCompanyId("company-1");
        summary.setClientId("client-1");
        summary.setTotalOwed(owed);
        summary.setTotalPaid(paid);
        summary.setTotalRemaining(owed.subtract(paid));
        return summary;
    }
}
