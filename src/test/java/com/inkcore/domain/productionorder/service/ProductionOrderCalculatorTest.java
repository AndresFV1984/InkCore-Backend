package com.inkcore.domain.productionorder.service;

import com.inkcore.domain.productionorder.model.FlipType;
import com.inkcore.domain.thousandrate.model.ThousandRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionOrderCalculatorTest {

    @Test
    void calculateGoodSizesUsesIntegerDivision() {
        assertEquals(250, ProductionOrderCalculator.calculateGoodSizes(1000, 4));
    }

    @Test
    void calculateThousandsUsesMinWhenBelowThreshold() {
        ThousandRate rate = ThousandRate.reconstitute(
                "r1", "c1", "Basico", "CMYK", 1000,
                new BigDecimal("100.00"), true, 500,
                new BigDecimal("0.50"), new BigDecimal("0.30"),
                new BigDecimal("10.00"), new BigDecimal("20.00"),
                false, LocalDate.of(2026, 1, 1)
        );
        assertEquals(new BigDecimal("0.50"), ProductionOrderCalculator.calculateThousands(100, rate));
    }

    @Test
    void postpressAppliesMinCost() {
        BigDecimal calculated = ProductionOrderCalculator.calculatePostpressPrice(
                new BigDecimal("0.10"), new BigDecimal("2.00"), 10);
        assertEquals(new BigDecimal("2.00"), calculated);
        assertTrue(ProductionOrderCalculator.appliesMinCost(calculated, new BigDecimal("5.00")));
        assertFalse(ProductionOrderCalculator.appliesMinCost(calculated, new BigDecimal("1.00")));
    }

    @Test
    void inkCountsMatchColors() {
        assertTrue(ProductionOrderCalculator.inkCountsMatchColors(2, 2, "4 COLORES"));
        assertFalse(ProductionOrderCalculator.inkCountsMatchColors(1, 1, "4"));
    }

    @Test
    void resolveFlipPriceFallsBackToBase() {
        BigDecimal price = ProductionOrderCalculator.resolveFlipPrice(
                FlipType.GRIPPER_FLIP,
                new BigDecimal("100"),
                null,
                new BigDecimal("130")
        );
        assertEquals(new BigDecimal("100"), price);
    }
}
