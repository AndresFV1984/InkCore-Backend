package com.inkcore.application.productionorder.usecase;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort;
import com.inkcore.domain.seller.ports.out.SellerRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductionOrderUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-15T12:00:00Z");

    @Mock ProductionOrderRepositoryPort repository;
    @Mock AuthenticatedCompanyResolver companyResolver;
    @Mock ClientRepositoryPort clientRepository;
    @Mock SellerRepositoryPort sellerRepository;

    private CreateProductionOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        ProductionOrderSupport support = new ProductionOrderSupport(
                repository,
                companyResolver,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
        useCase = new CreateProductionOrderUseCase(support, clientRepository, sellerRepository);
    }

    @Test
    void execute_createsOrderScopedToAuthenticatedCompany() {
        var auth = new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-seed-001");
        when(companyResolver.resolveUserId(auth)).thenReturn("user-1");
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(
                Client.reconstitute(
                        "client-1", "company-seed-001", "Cliente Demo", "NIT", "900",
                        null, null, null, null, null, null, true, LocalDate.of(2026, 1, 1)
                )
        ));
        when(repository.allocateNextOrderSequence("company-seed-001")).thenReturn(42L);
        when(repository.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

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
        assertNull(created.getVersion());
    }
}
