package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.ports.out.CustomerOrderRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductionOrderStatusUseCaseTest {

    @Mock ProductionOrderSupport support;
    @Mock CustomerOrderRepositoryPort customerOrderRepository;
    @Mock Authentication authentication;
    @Mock com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort repository;

    private UpdateProductionOrderStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateProductionOrderStatusUseCase(support, customerOrderRepository);
    }

    @Test
    void execute_persistsAnuladaWhenRequestSendsAnulada() {
        ProductionOrder order = order(1L);
        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-1");
        when(support.requireOrder("op-1", "company-1")).thenReturn(order);
        when(support.now()).thenReturn(LocalDateTime.of(2026, 9, 9, 12, 0));
        when(support.repository()).thenReturn(repository);
        when(repository.saveRoot(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerOrderRepository.findByProductionOrderId("company-1", "op-1")).thenReturn(Optional.empty());

        UpdateProductionOrderStatusUseCase.Result result = useCase.execute(
                "op-1",
                new UpdateProductionOrderStatusCommand(1L, "ANULADA", null),
                authentication
        );

        assertEquals("ANULADA", result.order().getStatus());
        assertNull(result.customerOrder());
        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void execute_normalizesCancelledAliasToAnulada() {
        ProductionOrder order = order(1L);
        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-1");
        when(support.requireOrder("op-1", "company-1")).thenReturn(order);
        when(support.now()).thenReturn(LocalDateTime.of(2026, 9, 9, 12, 0));
        when(support.repository()).thenReturn(repository);
        when(repository.saveRoot(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerOrderRepository.findByProductionOrderId("company-1", "op-1")).thenReturn(Optional.empty());

        useCase.execute(
                "op-1",
                new UpdateProductionOrderStatusCommand(1L, "CANCELLED", null),
                authentication
        );

        ArgumentCaptor<ProductionOrder> captor = ArgumentCaptor.forClass(ProductionOrder.class);
        verify(repository).saveRoot(captor.capture());
        assertEquals("ANULADA", captor.getValue().getStatus());
        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void execute_rejectsUnknownStatus() {
        ProductionOrder order = order(1L);
        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-1");
        when(support.requireOrder("op-1", "company-1")).thenReturn(order);

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        "op-1",
                        new UpdateProductionOrderStatusCommand(1L, "UNKNOWN", null),
                        authentication
                )
        );
    }

    @Test
    void execute_createsOrderWhenEnteringInProgress() {
        ProductionOrder order = order(1L);
        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-1");
        when(support.requireOrder("op-1", "company-1")).thenReturn(order);
        when(support.now()).thenReturn(LocalDateTime.of(2026, 9, 12, 10, 0));
        when(support.repository()).thenReturn(repository);
        when(repository.saveRoot(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerOrderRepository.findByProductionOrderId("company-1", "op-1")).thenReturn(Optional.empty());
        when(customerOrderRepository.allocateNextOdpSequence("company-1")).thenReturn(7L);
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProductionOrderStatusUseCase.Result result = useCase.execute(
                "op-1",
                new UpdateProductionOrderStatusCommand(1L, "IN_PROGRESS", null),
                authentication
        );

        assertEquals("IN_PROGRESS", result.order().getStatus());
        assertNotNull(result.customerOrder());
        assertEquals("ODP-7", result.customerOrder().getOdpNumber());
        assertEquals("op-1", result.customerOrder().getProductionOrderId());
        assertEquals("client-1", result.customerOrder().getClientId());
        verify(customerOrderRepository).save(any(CustomerOrder.class));
    }

    @Test
    void execute_doesNotDuplicateOrderWhenAlreadyInProgress() {
        ProductionOrder order = order(1L);
        order.setStatus("IN_PROGRESS_PREPRESS");
        CustomerOrder existing = CustomerOrder.createNew(
                "company-1", "ODP-3", "op-1", "client-1", "user-1", LocalDateTime.of(2026, 9, 1, 8, 0)
        );
        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-1");
        when(support.requireOrder("op-1", "company-1")).thenReturn(order);
        when(support.now()).thenReturn(LocalDateTime.of(2026, 9, 12, 10, 0));
        when(support.repository()).thenReturn(repository);
        when(repository.saveRoot(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerOrderRepository.findByProductionOrderId("company-1", "op-1"))
                .thenReturn(Optional.of(existing));

        UpdateProductionOrderStatusUseCase.Result result = useCase.execute(
                "op-1",
                new UpdateProductionOrderStatusCommand(1L, "IN_PROGRESS_PRINTING", null),
                authentication
        );

        assertEquals(existing, result.customerOrder());
        verify(customerOrderRepository, never()).save(any());
        verify(customerOrderRepository, never()).allocateNextOdpSequence(eq("company-1"));
    }

    private static ProductionOrder order(long version) {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("op-1");
        order.setCompanyId("company-1");
        order.setClientId("client-1");
        order.setVersion(version);
        order.setStatus(ProductionOrder.STATUS_PENDING);
        order.setState(true);
        return order;
    }
}
