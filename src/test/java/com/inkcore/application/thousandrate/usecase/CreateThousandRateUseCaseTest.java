package com.inkcore.application.thousandrate.usecase;

import com.inkcore.domain.thousandrate.exception.ThousandRateAlreadyExistsException;
import com.inkcore.domain.thousandrate.model.ThousandRate;
import com.inkcore.domain.thousandrate.ports.out.ThousandRateRepositoryPort;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateThousandRateUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-08T12:00:00Z");

    @Mock ThousandRateRepositoryPort thousandRateRepository;

    private CreateThousandRateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateThousandRateUseCase(
                thousandRateRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_success_defaultsThousandUnitStateAndIsDefault() {
        when(thousandRateRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Color básico")).thenReturn(false);
        when(thousandRateRepository.save(any(ThousandRate.class))).thenAnswer(inv -> inv.getArgument(0));

        ThousandRate created = useCase.execute(new CreateThousandRateCommand(
                "company-seed-001",
                "Color básico",
                "1 COLOR",
                null,
                new BigDecimal("17500"),
                null,
                600,
                new BigDecimal("500"),
                new BigDecimal("0.2"),
                null,
                null,
                null
        ));

        assertEquals("Color básico", created.getName());
        assertEquals("1 COLOR", created.getColorCategory());
        assertEquals(1000, created.getThousandUnit());
        assertEquals(new BigDecimal("17500.00"), created.getPrice());
        assertTrue(created.isState());
        assertFalse(created.isDefault());
        assertEquals(600, created.getMinThresholdUnits());
        assertEquals(new BigDecimal("500.00"), created.getMinThousand());
        assertEquals(new BigDecimal("0.20"), created.getDecimalThreshold());
        assertNull(created.getGripperFlipPrice());
        assertNull(created.getSquareFlipPrice());
        assertEquals(LocalDate.of(2026, 8, 8), created.getCreationDate());
        verify(thousandRateRepository, never()).clearDefaultForCompanyAndColorCategoryExcept(
                any(), any(), any());
    }

    @Test
    void execute_isDefault_clearsOtherDefaultsForSameColorCategory() {
        when(thousandRateRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Color básico")).thenReturn(false);
        when(thousandRateRepository.save(any(ThousandRate.class))).thenAnswer(inv -> inv.getArgument(0));

        ThousandRate created = useCase.execute(new CreateThousandRateCommand(
                "company-seed-001",
                "Color básico",
                "basico",
                1000,
                new BigDecimal("17500"),
                true,
                600,
                new BigDecimal("500"),
                new BigDecimal("0.2"),
                new BigDecimal("20000"),
                new BigDecimal("20000"),
                true
        ));

        assertTrue(created.isDefault());
        verify(thousandRateRepository).clearDefaultForCompanyAndColorCategoryExcept(
                "company-seed-001",
                "basico",
                created.getThousandRateId()
        );
    }

    @Test
    void execute_duplicateName_throwsConflict() {
        when(thousandRateRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Color básico")).thenReturn(true);

        assertThrows(ThousandRateAlreadyExistsException.class, () -> useCase.execute(
                new CreateThousandRateCommand(
                        "company-seed-001",
                        "Color básico",
                        "1 COLOR",
                        1000,
                        new BigDecimal("17500"),
                        true,
                        600,
                        new BigDecimal("500"),
                        new BigDecimal("0.2"),
                        new BigDecimal("20000"),
                        new BigDecimal("20000"),
                        false
                )));

        verify(thousandRateRepository, never()).save(any());
    }
}
