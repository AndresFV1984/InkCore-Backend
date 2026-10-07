package com.inkcore.domain.paper.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperRemnantTest {

    @Test
    void createNewDefaultsAvailableAndUnitCost() {
        PaperRemnant remnant = PaperRemnant.createNew(
                "company-1",
                "paper-1",
                new BigDecimal("35"),
                new BigDecimal("50"),
                "cm",
                new BigDecimal("12"),
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 10, 6),
                "  sobrante  ",
                null,
                LocalDateTime.of(2026, 10, 6, 12, 0)
        );

        assertEquals(new BigDecimal("12.00"), remnant.getQuantityInitial());
        assertEquals(new BigDecimal("12.00"), remnant.getQuantityAvailable());
        assertEquals(new BigDecimal("0.00"), remnant.getUnitCost());
        assertEquals("sobrante", remnant.getNote());
        assertTrue(remnant.isState());
        assertEquals("cm", remnant.getUnit());
    }

    @Test
    void rejectsAvailableGreaterThanInitial() {
        assertThrows(IllegalArgumentException.class, () -> PaperRemnant.createNew(
                "company-1",
                "paper-1",
                new BigDecimal("35"),
                new BigDecimal("50"),
                "cm",
                new BigDecimal("5"),
                new BigDecimal("6"),
                BigDecimal.ZERO,
                null,
                null,
                LocalDate.of(2026, 10, 6),
                null,
                true,
                LocalDateTime.of(2026, 10, 6, 12, 0)
        ));
    }
}
