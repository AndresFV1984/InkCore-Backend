package com.inkcore.application.papertype.usecase;

import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.model.PaperTypeSupplierAssignment;
import com.inkcore.domain.papertype.ports.out.PaperTypeRepositoryPort;
import com.inkcore.domain.supplier.model.Supplier;
import com.inkcore.domain.supplier.ports.out.SupplierRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePaperTypeUseCaseTest {

    private static final String PAPER_TYPE_ID = "paper-type-1";

    @Mock PaperTypeRepositoryPort paperTypeRepository;
    @Mock CutLayoutRepositoryPort cutLayoutRepository;
    @Mock SupplierRepositoryPort supplierRepository;

    private UpdatePaperTypeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdatePaperTypeUseCase(
                paperTypeRepository,
                cutLayoutRepository,
                supplierRepository
        );
    }

    @Test
    void execute_emptySuppliers_throws() {
        PaperType existing = existingPaperType();
        when(paperTypeRepository.findById(PAPER_TYPE_ID)).thenReturn(Optional.of(existing));
        when(paperTypeRepository.existsByCompanyIdAndNameIgnoreCaseExcludingPaperTypeId(
                "company-seed-001", "Bond 75g", PAPER_TYPE_ID)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(new UpdatePaperTypeCommand(
                PAPER_TYPE_ID,
                "Bond 75g",
                new BigDecimal("70"),
                new BigDecimal("100"),
                "cm",
                false,
                true,
                List.of(),
                List.of()
        )));

        verify(paperTypeRepository, never()).save(any());
    }

    @Test
    void execute_updatesSupplierPrices() {
        PaperType existing = existingPaperType();
        when(paperTypeRepository.findById(PAPER_TYPE_ID)).thenReturn(Optional.of(existing));
        when(paperTypeRepository.existsByCompanyIdAndNameIgnoreCaseExcludingPaperTypeId(
                "company-seed-001", "Bond 75g", PAPER_TYPE_ID)).thenReturn(false);
        when(supplierRepository.findById("sup-1")).thenReturn(Optional.of(
                Supplier.reconstitute(
                        "sup-1",
                        "company-seed-001",
                        "Papeles del Norte",
                        "NIT",
                        "900123456",
                        "Antioquia",
                        "Medellín",
                        "Calle 1",
                        "3001234567",
                        "contacto@ejemplo.com",
                        "Ana",
                        true,
                        LocalDate.of(2026, 8, 1)
                )
        ));
        when(paperTypeRepository.save(any(PaperType.class))).thenAnswer(inv -> inv.getArgument(0));

        PaperType updated = useCase.execute(new UpdatePaperTypeCommand(
                PAPER_TYPE_ID,
                "Bond 75g",
                new BigDecimal("70"),
                new BigDecimal("100"),
                "cm",
                false,
                true,
                List.of(),
                List.of(new PaperTypeSupplierAssignmentCommand("sup-1", new BigDecimal("1800"), 600))
        ));

        assertEquals(1, updated.getSupplierAssignments().size());
        assertEquals(new BigDecimal("1800.00"), updated.getSupplierAssignments().get(0).getSheetValue());
        assertEquals(600, updated.getSupplierAssignments().get(0).getPackageUnit());
    }

    private static PaperType existingPaperType() {
        return PaperType.reconstitute(
                PAPER_TYPE_ID,
                "company-seed-001",
                "Bond 75g",
                new BigDecimal("70"),
                new BigDecimal("100"),
                "cm",
                false,
                true,
                LocalDate.of(2026, 8, 1),
                List.of(),
                List.of(PaperTypeSupplierAssignment.of("sup-1", new BigDecimal("1500"), 500))
        );
    }
}
