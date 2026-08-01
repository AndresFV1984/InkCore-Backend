package com.inkcore.application.cutlayout.usecase;

import com.inkcore.domain.cutlayout.exception.CutLayoutAlreadyExistsException;
import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class CreateCutLayoutUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-01T12:00:00Z");

    @Mock CutLayoutRepositoryPort cutLayoutRepository;

    private CreateCutLayoutUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateCutLayoutUseCase(
                cutLayoutRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_success_defaultsUnitCmAndStateTrue() {
        when(cutLayoutRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Etiqueta")).thenReturn(false);
        when(cutLayoutRepository.save(any(CutLayout.class))).thenAnswer(inv -> inv.getArgument(0));

        CutLayout created = useCase.execute(new CreateCutLayoutCommand(
                "company-seed-001",
                "Etiqueta",
                new BigDecimal("10"),
                new BigDecimal("5"),
                null,
                24,
                null
        ));

        assertEquals("Etiqueta", created.getName());
        assertEquals(new BigDecimal("10.00"), created.getWidth());
        assertEquals(new BigDecimal("5.00"), created.getHeight());
        assertEquals("cm", created.getUnit());
        assertEquals(24, created.getPiecesPerSheet());
        assertTrue(created.isState());
        assertEquals(LocalDate.of(2026, 8, 1), created.getCreationDate());

        ArgumentCaptor<CutLayout> captor = ArgumentCaptor.forClass(CutLayout.class);
        verify(cutLayoutRepository).save(captor.capture());
        assertEquals("company-seed-001", captor.getValue().getCompanyId());
    }

    @Test
    void execute_duplicateName_throwsConflict() {
        when(cutLayoutRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Etiqueta")).thenReturn(true);

        assertThrows(CutLayoutAlreadyExistsException.class, () -> useCase.execute(new CreateCutLayoutCommand(
                "company-seed-001",
                "Etiqueta",
                new BigDecimal("10"),
                new BigDecimal("5"),
                "cm",
                24,
                true
        )));

        verify(cutLayoutRepository, never()).save(any());
    }

    @Test
    void execute_invalidUnit_throwsIllegalArgument() {
        when(cutLayoutRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Etiqueta")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new CreateCutLayoutCommand(
                "company-seed-001",
                "Etiqueta",
                new BigDecimal("10"),
                new BigDecimal("5"),
                "px",
                24,
                true
        )));

        verify(cutLayoutRepository, never()).save(any());
    }
}
