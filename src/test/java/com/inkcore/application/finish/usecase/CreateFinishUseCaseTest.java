package com.inkcore.application.finish.usecase;

import com.inkcore.domain.finish.exception.FinishAlreadyExistsException;
import com.inkcore.domain.finish.model.Finish;
import com.inkcore.domain.finish.ports.out.FinishRepositoryPort;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFinishUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-28T12:00:00Z");

    @Mock FinishRepositoryPort finishRepository;

    private CreateFinishUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateFinishUseCase(
                finishRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_success_defaultsValuePerCm2AndState() {
        when(finishRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Laminado mate")).thenReturn(false);
        when(finishRepository.save(any(Finish.class))).thenAnswer(inv -> inv.getArgument(0));

        Finish created = useCase.execute(new CreateFinishCommand(
                "company-seed-001",
                "Laminado mate",
                new BigDecimal("15000.00"),
                null,
                true,
                null
        ));

        assertEquals("Laminado mate", created.getName());
        assertEquals(new BigDecimal("15000.00"), created.getMinCost());
        assertEquals(new BigDecimal("0.00"), created.getValuePerCm2());
        assertTrue(created.isQuickAccess());
        assertTrue(created.isState());
        assertEquals(LocalDate.of(2026, 7, 28), created.getCreationDate());

        ArgumentCaptor<Finish> captor = ArgumentCaptor.forClass(Finish.class);
        verify(finishRepository).save(captor.capture());
        assertEquals("company-seed-001", captor.getValue().getCompanyId());
    }

    @Test
    void execute_duplicateName_throwsConflict() {
        when(finishRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Laminado mate")).thenReturn(true);

        assertThrows(FinishAlreadyExistsException.class, () -> useCase.execute(new CreateFinishCommand(
                "company-seed-001",
                "Laminado mate",
                null,
                BigDecimal.ZERO,
                false,
                true
        )));

        verify(finishRepository, never()).save(any());
    }

    @Test
    void execute_quickAccessDefaultsFalse() {
        when(finishRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "UV")).thenReturn(false);
        when(finishRepository.save(any(Finish.class))).thenAnswer(inv -> inv.getArgument(0));

        Finish created = useCase.execute(new CreateFinishCommand(
                "company-seed-001",
                "UV",
                null,
                new BigDecimal("8000"),
                null,
                true
        ));

        assertFalse(created.isQuickAccess());
        assertEquals(new BigDecimal("8000.00"), created.getValuePerCm2());
    }
}
