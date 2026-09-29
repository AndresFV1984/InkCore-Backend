package com.inkcore.domain.productionorder.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WasteValuationTest {

    @Test
    void plannedQuantityUsesPercentageOfSheets() {
        assertEquals(0, new BigDecimal("20.00").compareTo(
                WasteValuation.plannedQuantity(1000, new BigDecimal("2"))));
    }

    @Test
    void plannedQuantityAddsFixedSheetsToThePercentage() {
        assertEquals(0, new BigDecimal("420.00").compareTo(
                WasteValuation.plannedQuantity(1000, new BigDecimal("2"), new BigDecimal("400"))));
    }

    @Test
    void plannedQuantityWithZeroFixedSheetsMatchesThePercentageOnly() {
        assertEquals(0, new BigDecimal("20.00").compareTo(
                WasteValuation.plannedQuantity(1000, new BigDecimal("2"), BigDecimal.ZERO)));
    }

    @Test
    void missingPercentageFallsBackToCompanyDefault() {
        assertEquals(0, new BigDecimal("3.00").compareTo(
                WasteValuation.percentage(null, new BigDecimal("3"))));
    }

    @Test
    void rejectsPercentageOutsideZeroToHundred() {
        assertThrows(IllegalArgumentException.class,
                () -> WasteValuation.percentage(new BigDecimal("101"), new BigDecimal("2")));
    }

    @Test
    void allocatesObservedWasteUpToThePlanAndLeavesTheExcess() {
        WasteValuation.Allocation within = WasteValuation.allocate(
                List.of(new BigDecimal("6"), new BigDecimal("4")),
                new BigDecimal("7")
        );
        assertEquals(0, new BigDecimal("6.00").compareTo(within.assignedToPlanned().get(0)));
        assertEquals(0, new BigDecimal("1.00").compareTo(within.assignedToPlanned().get(1)));
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(within.excess()));

        WasteValuation.Allocation over = WasteValuation.allocate(
                List.of(new BigDecimal("6"), new BigDecimal("4")),
                new BigDecimal("12")
        );
        assertEquals(0, new BigDecimal("2.00").compareTo(over.excess()));
    }

    @Test
    void proratesMachineAndInkOntoTheWastedUnit() {
        BigDecimal unit = WasteValuation.proratedUnitCost(
                new BigDecimal("100"),
                new BigDecimal("600"),
                new BigDecimal("300"),
                new BigDecimal("100")
        );
        assertEquals(0, new BigDecimal("109.00").compareTo(unit));
    }
}
