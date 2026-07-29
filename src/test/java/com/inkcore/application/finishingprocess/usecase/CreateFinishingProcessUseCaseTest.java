package com.inkcore.application.finishingprocess.usecase;

import com.inkcore.domain.finishingprocess.exception.FinishingProcessAlreadyExistsException;
import com.inkcore.domain.finishingprocess.model.FinishingProcess;
import com.inkcore.domain.finishingprocess.ports.out.FinishingProcessRepositoryPort;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFinishingProcessUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-28T12:00:00Z");

    @Mock FinishingProcessRepositoryPort finishingProcessRepository;

    private CreateFinishingProcessUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateFinishingProcessUseCase(
                finishingProcessRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_success_defaultsValuePerCm2AndState() {
        when(finishingProcessRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Plegar")).thenReturn(false);
        when(finishingProcessRepository.save(any(FinishingProcess.class))).thenAnswer(inv -> inv.getArgument(0));

        FinishingProcess created = useCase.execute(new CreateFinishingProcessCommand(
                "company-seed-001",
                "Plegar",
                new BigDecimal("5000.00"),
                null,
                true,
                null
        ));

        assertEquals("Plegar", created.getName());
        assertEquals(new BigDecimal("5000.00"), created.getMinCost());
        assertEquals(new BigDecimal("0.00"), created.getValuePerCm2());
        assertTrue(created.isQuickAccess());
        assertTrue(created.isState());
        assertEquals(LocalDate.of(2026, 7, 28), created.getCreationDate());
    }

    @Test
    void execute_duplicateName_throwsConflict() {
        when(finishingProcessRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Plegar")).thenReturn(true);

        assertThrows(FinishingProcessAlreadyExistsException.class, () -> useCase.execute(
                new CreateFinishingProcessCommand(
                        "company-seed-001",
                        "Plegar",
                        null,
                        BigDecimal.ZERO,
                        false,
                        true
                )));

        verify(finishingProcessRepository, never()).save(any());
    }

    @Test
    void execute_quickAccessDefaultsFalse() {
        when(finishingProcessRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Embolsar")).thenReturn(false);
        when(finishingProcessRepository.save(any(FinishingProcess.class))).thenAnswer(inv -> inv.getArgument(0));

        FinishingProcess created = useCase.execute(new CreateFinishingProcessCommand(
                "company-seed-001",
                "Embolsar",
                null,
                new BigDecimal("800"),
                null,
                true
        ));

        assertFalse(created.isQuickAccess());
        assertEquals(new BigDecimal("800.00"), created.getValuePerCm2());
    }
}
