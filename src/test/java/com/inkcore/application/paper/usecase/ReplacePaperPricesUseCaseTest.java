package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.Paper;
import com.inkcore.domain.paper.model.PaperSupplierPrice;
import com.inkcore.domain.paper.ports.out.PaperSupplierPriceRepositoryPort;
import com.inkcore.domain.supplier.model.Supplier;
import com.inkcore.domain.supplier.ports.out.SupplierRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplacePaperPricesUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-22T12:00:00Z");

    @Mock PaperSupplierPriceRepositoryPort priceRepository;
    @Mock PaperAccess access;
    @Mock PaperSupport support;
    @Mock SupplierRepositoryPort supplierRepository;

    private ReplacePaperPricesUseCase useCase;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        useCase = new ReplacePaperPricesUseCase(
                priceRepository,
                access,
                support,
                supplierRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
        auth = new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
        when(support.companyId(auth)).thenReturn("company-1");
        when(support.userId(auth)).thenReturn("user-1");
    }

    @Test
    void execute_syncsDiffAndAutoMarksSinglePreferredWhenNoneProvided() {
        Paper paper = Paper.reconstitute(
                "paper-1", "company-1", "Bond", new BigDecimal("75"),
                new BigDecimal("70"), new BigDecimal("100"), "cm",
                false, false, null, null, null, true,
                LocalDate.of(2026, 1, 1), LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        when(access.requirePaper("paper-1", "company-1")).thenReturn(paper);
        when(supplierRepository.findById("sup-a")).thenReturn(Optional.of(supplier("sup-a")));
        when(supplierRepository.findById("sup-b")).thenReturn(Optional.of(supplier("sup-b")));

        PaperSupplierPrice existing = PaperSupplierPrice.reconstitute(
                "price-a", "company-1", "paper-1", "sup-a",
                new BigDecimal("100"), 500, BigDecimal.ZERO,
                null, null, null, LocalDate.of(2026, 1, 1), true, true,
                new BigDecimal("100"), LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        when(priceRepository.findByPaperId("company-1", "paper-1")).thenReturn(List.of(existing));
        when(priceRepository.replaceDiff(eq("company-1"), eq("paper-1"), anyList(), eq("user-1")))
                .thenAnswer(inv -> inv.getArgument(2));

        var command = new PaperCommands.ReplacePaperPricesCommand(List.of(
                new PaperCommands.ReplacePaperPriceItem(
                        "sup-a", new BigDecimal("100"), 500, BigDecimal.ZERO,
                        null, null, null, null, false, true
                ),
                new PaperCommands.ReplacePaperPriceItem(
                        "sup-b", new BigDecimal("80"), 500, BigDecimal.ZERO,
                        null, null, null, null, false, true
                )
        ));

        List<PaperSupplierPrice> result = useCase.execute("paper-1", command, auth);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PaperSupplierPrice>> desiredCaptor = ArgumentCaptor.forClass(List.class);
        verify(priceRepository).replaceDiff(
                eq("company-1"), eq("paper-1"), desiredCaptor.capture(), eq("user-1"));
        List<PaperSupplierPrice> desired = desiredCaptor.getValue();
        assertEquals(2, desired.size());
        long preferredCount = desired.stream().filter(PaperSupplierPrice::isPreferred).count();
        assertEquals(1, preferredCount);
        PaperSupplierPrice preferred = desired.stream().filter(PaperSupplierPrice::isPreferred).findFirst().orElseThrow();
        assertEquals("sup-b", preferred.getSupplierId());
        assertEquals(2, result.size());
    }

    @Test
    void execute_rejectsMultiplePreferredSuppliers() {
        Paper paper = Paper.reconstitute(
                "paper-1", "company-1", "Bond", new BigDecimal("75"),
                new BigDecimal("70"), new BigDecimal("100"), "cm",
                false, false, null, null, null, true,
                LocalDate.of(2026, 1, 1), LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        when(access.requirePaper("paper-1", "company-1")).thenReturn(paper);
        when(supplierRepository.findById("sup-a")).thenReturn(Optional.of(supplier("sup-a")));
        when(supplierRepository.findById("sup-b")).thenReturn(Optional.of(supplier("sup-b")));
        when(priceRepository.findByPaperId("company-1", "paper-1")).thenReturn(List.of());

        var command = new PaperCommands.ReplacePaperPricesCommand(List.of(
                new PaperCommands.ReplacePaperPriceItem(
                        "sup-a", new BigDecimal("100"), 500, BigDecimal.ZERO,
                        null, null, null, null, true, true
                ),
                new PaperCommands.ReplacePaperPriceItem(
                        "sup-b", new BigDecimal("80"), 500, BigDecimal.ZERO,
                        null, null, null, null, true, true
                )
        ));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute("paper-1", command, auth)
        );
        assertTrue(ex.getMessage().contains("preferido"));
    }

    private static Supplier supplier(String id) {
        return Supplier.reconstitute(
                id, "company-1", "Proveedor " + id,
                "NIT", "900", "Antioquia", "Medellín",
                "Calle 1", "300", null, null, true, LocalDate.of(2026, 1, 1)
        );
    }
}
