package com.inkcore.application.productionorder.usecase;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.machine.model.MachineType;
import com.inkcore.domain.machine.ports.out.MachineRepositoryPort;
import com.inkcore.domain.productionorder.model.MachineUsage;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.productionorder.ports.out.MachineUsageRepositoryPort;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort;
import com.inkcore.domain.seller.ports.out.SellerRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductionOrderUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-15T12:00:00Z");

    @Mock ProductionOrderRepositoryPort repository;
    @Mock AuthenticatedCompanyResolver companyResolver;
    @Mock ClientRepositoryPort clientRepository;
    @Mock SellerRepositoryPort sellerRepository;
    @Mock MachineRepositoryPort machineRepository;
    @Mock MachineUsageRepositoryPort machineUsageRepository;

    private CreateProductionOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        ProductionOrderSupport support = new ProductionOrderSupport(
                repository,
                companyResolver,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
        useCase = new CreateProductionOrderUseCase(
                support, clientRepository, sellerRepository, machineRepository, machineUsageRepository);
    }

    @Test
    void execute_createsOrderScopedToAuthenticatedCompany() {
        var auth = new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-seed-001");
        when(companyResolver.resolveUserId(auth)).thenReturn("user-1");
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(
                Client.reconstitute(
                        "client-1", "company-seed-001", "Cliente Demo", "NIT", "900",
                        null, null, null, null, null, null, 0, true, LocalDate.of(2026, 1, 1)
                )
        ));
        when(repository.allocateNextOrderSequence("company-seed-001")).thenReturn(42L);
        when(repository.allocateNextPriorityRank("company-seed-001")).thenReturn(3);
        when(repository.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(machineRepository.findPreferredActiveByCompanyId("company-seed-001")).thenReturn(List.of());
        when(machineUsageRepository.findByProductionOrderId(eq("company-seed-001"), any()))
                .thenReturn(List.of());

        ProductionOrder created = useCase.execute(
                new CreateProductionOrderCommand(
                        "client-1", "Brochure", null, null, 1000, null, null, null
                ),
                auth
        );

        assertEquals("company-seed-001", created.getCompanyId());
        assertEquals("Brochure", created.getWorkName());
        assertEquals(1000, created.getRequestedQuantity());
        assertEquals("OP-42", created.getOrderNumber());
        assertEquals(3, created.getPriorityRank());
        assertNull(created.getVersion());
        assertEquals(1, created.getOperators().size());
        assertEquals(
                "user-1",
                created.getOperators().stream()
                        .filter(op -> op.getStage() == ProductionOrderStage.BILLING)
                        .map(op -> op.getUserId())
                        .findFirst()
                        .orElse(null)
        );
    }

    @Test
    void execute_preloadsPreferredMachinePerStageWithCatalogSetupMinutes() {
        var auth = new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-seed-001");
        when(companyResolver.resolveUserId(auth)).thenReturn("user-1");
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(
                Client.reconstitute(
                        "client-1", "company-seed-001", "Cliente Demo", "NIT", "900",
                        null, null, null, null, null, null, 0, true, LocalDate.of(2026, 1, 1)
                )
        ));
        when(repository.allocateNextOrderSequence("company-seed-001")).thenReturn(1L);
        when(repository.allocateNextPriorityRank("company-seed-001")).thenReturn(1);
        when(repository.save(any(ProductionOrder.class))).thenAnswer(inv -> {
            ProductionOrder order = inv.getArgument(0);
            order.setProductionOrderId("po-1");
            return order;
        });

        Machine preferredPrint = Machine.reconstitute(
                "machine-print-1",
                "company-seed-001",
                "Offset",
                MachineType.IMPRESION,
                null,
                null,
                new BigDecimal("100000"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1600"),
                new BigDecimal("50.00"),
                15,
                true,
                true,
                LocalDate.of(2026, 1, 1),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        when(machineRepository.findPreferredActiveByCompanyId("company-seed-001"))
                .thenReturn(List.of(preferredPrint));
        when(machineUsageRepository.replacePhase(eq("po-1"), eq("impresion"), anyList()))
                .thenAnswer(inv -> inv.getArgument(2));
        when(machineUsageRepository.findByProductionOrderId("company-seed-001", "po-1"))
                .thenReturn(List.of());

        useCase.execute(
                new CreateProductionOrderCommand(
                        "client-1", "Flyer", null, null, 500, null, null, null
                ),
                auth
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MachineUsage>> usagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(machineUsageRepository).replacePhase(eq("po-1"), eq("impresion"), usagesCaptor.capture());
        MachineUsage usage = usagesCaptor.getValue().get(0);
        assertEquals("machine-print-1", usage.getMachineId());
        assertEquals(15, usage.getEstimatedSetupMinutes());
        assertEquals(0, usage.getEstimatedRunMinutes());
        assertEquals("impresion", usage.getPhase());
    }
}
