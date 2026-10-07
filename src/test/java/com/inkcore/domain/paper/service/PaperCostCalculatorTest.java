package com.inkcore.domain.paper.service;

import com.inkcore.domain.paper.model.PriceRule;
import com.inkcore.domain.wastesettings.model.CompanyWasteSettings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperCostCalculatorTest {

    private static CompanyWasteSettings wasteSettings() {
        return CompanyWasteSettings.initialSuggestion("company-1");
    }

    @Test
    void quantityOneProducesSingleNetSheetWithDefaultWaste() {
        PaperCostCalculator.CostBreakdown breakdown = PaperCostCalculator.calculate(
                1,
                1,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                true,
                new BigDecimal("10.00"),
                wasteSettings(),
                null,
                new BigDecimal("19"),
                true
        );
        assertEquals(1, breakdown.netSheets());
        assertTrue(breakdown.paperCost().compareTo(new BigDecimal("100.00")) >= 0);
        assertTrue(breakdown.cutCost().compareTo(new BigDecimal("10.00")) >= 0);
    }

    @Test
    void nonExactDivisionCeilsNetSheets() {
        PaperCostCalculator.CostBreakdown breakdown = PaperCostCalculator.calculate(
                5,
                2,
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                true,
                BigDecimal.ZERO,
                wasteSettings(),
                null,
                new BigDecimal("19"),
                true
        );
        assertEquals(3, breakdown.netSheets());
    }

    @Test
    void rejectsInvalidPiecesPerSheet() {
        assertThrows(IllegalArgumentException.class, () -> PaperCostCalculator.calculate(
                10, 0, BigDecimal.ONE, BigDecimal.ZERO, true, BigDecimal.ZERO,
                wasteSettings(), null, new BigDecimal("19"), true));
    }

    @Test
    void appliesMakereadyAndPercentageWaste() {
        CompanyWasteSettings settings = CompanyWasteSettings.initialSuggestion("c1");
        PaperCostCalculator.CostBreakdown breakdown = PaperCostCalculator.calculate(
                1000,
                10,
                new BigDecimal("1.00"),
                BigDecimal.ZERO,
                true,
                BigDecimal.ZERO,
                settings,
                new BigDecimal("2.00"),
                new BigDecimal("19"),
                true
        );
        assertTrue(breakdown.wasteSheets().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(breakdown.makereadySheetsApplied().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void rejectsWasteOverrideOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> PaperCostCalculator.calculate(
                100,
                10,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                true,
                BigDecimal.ZERO,
                wasteSettings(),
                new BigDecimal("99"),
                new BigDecimal("19"),
                true
        ));
    }

    @Test
    void minPurchaseDiscardsCandidate() {
        var candidate = new PaperCostCalculator.PriceCandidate(
                "s1", "A", new BigDecimal("1"), BigDecimal.ZERO, new BigDecimal("1"),
                true, 500, null, false, false, 0
        );
        var options = PaperCostCalculator.selectAndQuote(
                List.of(candidate),
                PriceRule.BEST_COST,
                10,
                10,
                BigDecimal.ZERO,
                wasteSettings(),
                null,
                null,
                new BigDecimal("19"),
                true
        );
        assertFalse(options.get(0).accepted());
        assertTrue(options.get(0).discardReason().contains("mínimo"));
    }

    @Test
    void stalePriceAddsWarning() {
        var candidate = new PaperCostCalculator.PriceCandidate(
                "s1", "A", new BigDecimal("1"), BigDecimal.ZERO, new BigDecimal("1"),
                true, null, null, true, true, 45
        );
        var options = PaperCostCalculator.selectAndQuote(
                List.of(candidate),
                PriceRule.PREFERRED,
                100,
                10,
                BigDecimal.ZERO,
                wasteSettings(),
                null,
                null,
                new BigDecimal("19"),
                true
        );
        assertTrue(options.get(0).warnings().stream().anyMatch(w -> w.contains("Precio vencido")));
    }

    @Test
    void bestCostTieBreaksBySupplierId() {
        var lowA = new PaperCostCalculator.PriceCandidate(
                "supplier-b", "B", new BigDecimal("1.00"), BigDecimal.ZERO, new BigDecimal("1.00"),
                true, null, null, false, false, 0
        );
        var lowB = new PaperCostCalculator.PriceCandidate(
                "supplier-a", "A", new BigDecimal("1.00"), BigDecimal.ZERO, new BigDecimal("1.00"),
                true, null, null, false, false, 0
        );
        var options = PaperCostCalculator.selectAndQuote(
                List.of(lowA, lowB),
                PriceRule.BEST_COST,
                100,
                10,
                BigDecimal.ZERO,
                wasteSettings(),
                null,
                null,
                new BigDecimal("19"),
                true
        );
        assertEquals("supplier-a", options.get(0).price().supplierId());
    }
}
