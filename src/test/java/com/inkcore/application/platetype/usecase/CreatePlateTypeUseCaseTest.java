package com.inkcore.application.platetype.usecase;

import com.inkcore.domain.platetype.exception.PlateTypeAlreadyExistsException;
import com.inkcore.domain.platetype.model.PlateType;
import com.inkcore.domain.platetype.ports.out.PlateTypeRepositoryPort;
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
class CreatePlateTypeUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-04T12:00:00Z");

    @Mock PlateTypeRepositoryPort plateTypeRepository;

    private CreatePlateTypeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreatePlateTypeUseCase(
                plateTypeRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_success_defaultsUnitAndState() {
        when(plateTypeRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Plancha estándar")).thenReturn(false);
        when(plateTypeRepository.save(any(PlateType.class))).thenAnswer(inv -> inv.getArgument(0));

        PlateType created = useCase.execute(new CreatePlateTypeCommand(
                "company-seed-001",
                "Plancha estándar",
                new BigDecimal("10"),
                new BigDecimal("5"),
                null,
                new BigDecimal("185000"),
                null
        ));

        assertEquals("Plancha estándar", created.getName());
        assertEquals(new BigDecimal("10.00"), created.getWidth());
        assertEquals(new BigDecimal("5.00"), created.getHeight());
        assertEquals("cm", created.getUnit());
        assertEquals(new BigDecimal("185000.00"), created.getValue());
        assertTrue(created.isState());
        assertEquals(LocalDate.of(2026, 8, 4), created.getCreationDate());
    }

    @Test
    void execute_duplicateName_throwsConflict() {
        when(plateTypeRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Plancha estándar")).thenReturn(true);

        assertThrows(PlateTypeAlreadyExistsException.class, () -> useCase.execute(
                new CreatePlateTypeCommand(
                        "company-seed-001",
                        "Plancha estándar",
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        "cm",
                        new BigDecimal("185000"),
                        true
                )));

        verify(plateTypeRepository, never()).save(any());
    }
}
