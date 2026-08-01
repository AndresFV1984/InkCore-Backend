package com.inkcore.application.papertype.usecase;

import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.papertype.exception.PaperTypeAlreadyExistsException;
import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.ports.out.PaperTypeRepositoryPort;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePaperTypeUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-01T12:00:00Z");

    @Mock PaperTypeRepositoryPort paperTypeRepository;
    @Mock CutLayoutRepositoryPort cutLayoutRepository;

    private CreatePaperTypeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreatePaperTypeUseCase(
                paperTypeRepository,
                cutLayoutRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_success_defaultsUnitCoatedAndState() {
        when(paperTypeRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Bond 75g")).thenReturn(false);
        when(paperTypeRepository.save(any(PaperType.class))).thenAnswer(inv -> inv.getArgument(0));

        PaperType created = useCase.execute(new CreatePaperTypeCommand(
                "company-seed-001",
                "Bond 75g",
                new BigDecimal("70"),
                new BigDecimal("100"),
                null,
                new BigDecimal("1500"),
                500,
                null,
                null,
                List.of()
        ));

        assertEquals("Bond 75g", created.getName());
        assertEquals("cm", created.getUnit());
        assertEquals(new BigDecimal("1500.00"), created.getSheetValue());
        assertEquals(500, created.getPackageUnit());
        assertFalse(created.isCoated());
        assertTrue(created.isState());
        assertEquals(LocalDate.of(2026, 8, 1), created.getCreationDate());
        assertTrue(created.getCutAssignments().isEmpty());

        ArgumentCaptor<PaperType> captor = ArgumentCaptor.forClass(PaperType.class);
        verify(paperTypeRepository).save(captor.capture());
        assertEquals("company-seed-001", captor.getValue().getCompanyId());
    }

    @Test
    void execute_withCutLayout_assignsCutValue() {
        when(paperTypeRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Bond 75g")).thenReturn(false);
        when(cutLayoutRepository.findById("cut-1")).thenReturn(Optional.of(
                CutLayout.reconstitute(
                        "cut-1",
                        "company-seed-001",
                        "Etiqueta",
                        new BigDecimal("10.00"),
                        new BigDecimal("5.00"),
                        "cm",
                        24,
                        true,
                        LocalDate.of(2026, 8, 1)
                )
        ));
        when(paperTypeRepository.save(any(PaperType.class))).thenAnswer(inv -> inv.getArgument(0));

        PaperType created = useCase.execute(new CreatePaperTypeCommand(
                "company-seed-001",
                "Bond 75g",
                new BigDecimal("70"),
                new BigDecimal("100"),
                "cm",
                new BigDecimal("1500"),
                500,
                true,
                true,
                List.of(new PaperTypeCutAssignmentCommand("cut-1", new BigDecimal("200")))
        ));

        assertEquals(1, created.getCutAssignments().size());
        assertEquals("cut-1", created.getCutAssignments().get(0).getCutLayoutId());
        assertEquals(new BigDecimal("200.00"), created.getCutAssignments().get(0).getCutValue());
        assertTrue(created.isCoated());
    }

    @Test
    void execute_duplicateName_throwsConflict() {
        when(paperTypeRepository.existsByCompanyIdAndNameIgnoreCase(
                "company-seed-001", "Bond 75g")).thenReturn(true);

        assertThrows(PaperTypeAlreadyExistsException.class, () -> useCase.execute(new CreatePaperTypeCommand(
                "company-seed-001",
                "Bond 75g",
                new BigDecimal("70"),
                new BigDecimal("100"),
                "cm",
                new BigDecimal("1500"),
                500,
                false,
                true,
                List.of()
        )));

        verify(paperTypeRepository, never()).save(any());
    }
}
