package com.inkcore.application.assemblyprice.usecase;

import com.inkcore.domain.assemblyprice.exception.AssemblyPriceAlreadyExistsException;
import com.inkcore.domain.assemblyprice.model.AssemblyPrice;
import com.inkcore.domain.assemblyprice.ports.out.AssemblyPriceRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAssemblyPriceUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-08T12:00:00Z");

    @Mock AssemblyPriceRepositoryPort assemblyPriceRepository;

    private CreateAssemblyPriceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateAssemblyPriceUseCase(
                assemblyPriceRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_success_defaultsState() {
        when(assemblyPriceRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Montaje estándar 4 tintas")).thenReturn(false);
        when(assemblyPriceRepository.save(any(AssemblyPrice.class))).thenAnswer(inv -> inv.getArgument(0));

        AssemblyPrice created = useCase.execute(new CreateAssemblyPriceCommand(
                "company-seed-001",
                "Montaje estándar 4 tintas",
                new BigDecimal("85000"),
                null
        ));

        assertEquals("Montaje estándar 4 tintas", created.getName());
        assertEquals(new BigDecimal("85000.00"), created.getCost());
        assertTrue(created.isState());
        assertEquals(LocalDate.of(2026, 8, 8), created.getCreationDate());
    }

    @Test
    void execute_duplicateName_throwsConflict() {
        when(assemblyPriceRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Montaje estándar 4 tintas")).thenReturn(true);

        assertThrows(AssemblyPriceAlreadyExistsException.class, () -> useCase.execute(
                new CreateAssemblyPriceCommand(
                        "company-seed-001",
                        "Montaje estándar 4 tintas",
                        new BigDecimal("85000"),
                        true
                )));

        verify(assemblyPriceRepository, never()).save(any());
    }
}
