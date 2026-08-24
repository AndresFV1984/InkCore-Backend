package com.inkcore.application.productionorder.usecase;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.productionorder.exception.ProductionOrderNotDeletableException;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteProductionOrderUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-15T12:00:00Z");

    @Mock ProductionOrderRepositoryPort repository;
    @Mock AuthenticatedCompanyResolver companyResolver;

    private DeleteProductionOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        ProductionOrderSupport support = new ProductionOrderSupport(
                repository,
                companyResolver,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
        useCase = new DeleteProductionOrderUseCase(support);
    }

    @Test
    void execute_rejectsWhenOperatorsExist() {
        var auth = new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
        ProductionOrder order = ProductionOrder.createNew(
                "company-seed-001", "OP-1", "client-1", "Work", null,
                null, 100, null, null, "user-1",
                LocalDateTime.ofInstant(FIXED_NOW, ZoneOffset.UTC)
        );
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-seed-001");
        when(repository.findById(order.getProductionOrderId())).thenReturn(Optional.of(order));
        when(repository.hasOperators(order.getProductionOrderId())).thenReturn(true);

        assertThrows(ProductionOrderNotDeletableException.class,
                () -> useCase.execute(order.getProductionOrderId(), auth));
        verify(repository, never()).deleteById(order.getProductionOrderId());
    }

    @Test
    void execute_deletesWhenEligible() {
        var auth = new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
        ProductionOrder order = ProductionOrder.createNew(
                "company-seed-001", "OP-2", "client-1", "Work", null,
                null, 100, null, null, "user-1",
                LocalDateTime.ofInstant(FIXED_NOW, ZoneOffset.UTC)
        );
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-seed-001");
        when(repository.findById(order.getProductionOrderId())).thenReturn(Optional.of(order));
        when(repository.hasOperators(order.getProductionOrderId())).thenReturn(false);
        when(repository.isBillingCompleted(order.getProductionOrderId())).thenReturn(false);

        useCase.execute(order.getProductionOrderId(), auth);
        verify(repository).deleteById(order.getProductionOrderId());
    }
}
