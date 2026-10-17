package com.inkcore.application.productionorder.usecase;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReorderProductionOrdersUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-16T15:00:00Z");

    @Mock ProductionOrderRepositoryPort repository;
    @Mock AuthenticatedCompanyResolver companyResolver;

    private ReorderProductionOrdersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReorderProductionOrdersUseCase(new ProductionOrderSupport(
                repository,
                companyResolver,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        ));
    }

    @Test
    void execute_assignsRanksInTheRequestedOrder() {
        var auth = new UsernamePasswordAuthenticationToken("admin-1", "n/a", List.of());
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-1");
        when(companyResolver.resolveUserId(auth)).thenReturn("admin-1");
        when(repository.findOpenWorkQueueIds("company-1")).thenReturn(List.of("op-a", "op-b"));

        var result = useCase.execute(List.of("op-b", "op-a"), auth);

        assertEquals(List.of(
                new ReorderProductionOrdersUseCase.PriorityAssignment("op-b", 1),
                new ReorderProductionOrdersUseCase.PriorityAssignment("op-a", 2)
        ), result);
        verify(repository).replaceWorkQueuePriority(
                "company-1",
                List.of("op-b", "op-a"),
                "admin-1",
                LocalDateTime.ofInstant(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_rejectsAPartialQueue() {
        var auth = new UsernamePasswordAuthenticationToken("admin-1", "n/a", List.of());
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-1");
        when(companyResolver.resolveUserId(auth)).thenReturn("admin-1");
        when(repository.findOpenWorkQueueIds("company-1")).thenReturn(List.of("op-a", "op-b"));

        assertThrows(
                ProductionOrderBusinessRuleException.class,
                () -> useCase.execute(List.of("op-a"), auth)
        );
    }
}
