package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.order.exception.InsufficientAvailabilityException;
import com.inkcore.domain.order.model.AccountsReceivable;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderDeliveryUseCaseTest {

    @Mock OrderSupport support;
    @Mock OrderDeliveryRepositoryPort deliveryRepository;
    @Mock AccountsReceivableRepositoryPort accountsReceivableRepository;
    @Mock ClientRepositoryPort clientRepository;
    @Mock Authentication authentication;

    private CreateOrderDeliveryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateOrderDeliveryUseCase(
                support,
                deliveryRepository,
                accountsReceivableRepository,
                clientRepository
        );
    }

    @Test
    void execute_calculatesValueAndSnapshotsWithoutChangingProductionStatus() {
        ProductionOrder order = order();
        AccountsReceivable updated = new AccountsReceivable();
        updated.setProductionOrderId("op-1");
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 10, 0);

        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-authenticated");
        when(support.now()).thenReturn(now);
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(order);
        when(support.nextDeliveryNumber("company-1")).thenReturn("ODP-1");
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(client()));
        when(deliveryRepository.save(any(OrderDelivery.class))).thenAnswer(invocation -> {
            OrderDelivery delivery = invocation.getArgument(0);
            delivery.setAvailableBefore(1000);
            return delivery;
        });
        when(accountsReceivableRepository.findByProductionOrderId("company-1", "op-1"))
                .thenReturn(Optional.of(updated));

        useCase.execute(
                new CreateOrderDeliveryUseCase.CreateOrderDeliveryCommand(
                        "op-1",
                        "total",
                        400,
                        new BigDecimal("1000"),
                        "seller-1",
                        null,
                        "Entrega"
                ),
                authentication
        );

        ArgumentCaptor<OrderDelivery> captor = ArgumentCaptor.forClass(OrderDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        OrderDelivery saved = captor.getValue();
        assertEquals("ODP-1", saved.getDeliveryNumber());
        assertEquals(0, new BigDecimal("400000.00").compareTo(saved.getTotalValue()));
        assertEquals("Trabajo demo", saved.getWorkNameSnapshot());
        assertEquals("Cliente demo", saved.getClientNameSnapshot());
        assertEquals("user-authenticated", saved.getDeliveredBy());
        assertEquals("IN_PROGRESS", order.getStatus());
        verify(support, never()).productionOrderRepository();
    }

    @Test
    void execute_translatesDatabaseAvailabilityError() {
        ProductionOrder order = order();
        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-authenticated");
        when(support.now()).thenReturn(LocalDateTime.of(2026, 9, 8, 10, 0));
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(order);
        when(support.nextDeliveryNumber("company-1")).thenReturn("ODP-2");
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(client()));
        when(deliveryRepository.save(any(OrderDelivery.class))).thenThrow(
                new DataIntegrityViolationException(
                        "trigger",
                        new RuntimeException(
                                "No se puede entregar 500 unidades: solo hay 300 disponibles para la OP OP-42"
                        )
                )
        );

        InsufficientAvailabilityException error = assertThrows(
                InsufficientAvailabilityException.class,
                () -> useCase.execute(
                        new CreateOrderDeliveryUseCase.CreateOrderDeliveryCommand(
                                "op-1", "parcial", 500, BigDecimal.ONE,
                                null, null, null
                        ),
                        authentication
                )
        );

        assertEquals("INSUFFICIENT_AVAILABILITY", error.getCode());
        assertEquals(
                "No se puede entregar 500 unidades: solo hay 300 disponibles para la OP OP-42",
                error.getMessage()
        );
    }

    private static ProductionOrder order() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("op-1");
        order.setCompanyId("company-1");
        order.setClientId("client-1");
        order.setWorkName("Trabajo demo");
        order.setStatus("IN_PROGRESS");
        order.setState(true);
        return order;
    }

    private static Client client() {
        return Client.reconstitute(
                "client-1",
                "company-1",
                "Cliente demo",
                "NIT",
                "900",
                "Antioquia",
                "Medellín",
                null,
                null,
                null,
                null,
                0,
                true,
                LocalDate.of(2026, 1, 1)
        );
    }
}
