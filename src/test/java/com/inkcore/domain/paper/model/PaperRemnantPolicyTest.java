package com.inkcore.domain.paper.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperRemnantPolicyTest {

    @Test
    void createRequiresMinDimsWhenAcceptsRemnants() {
        assertThrows(IllegalArgumentException.class, () -> Paper.createNew(
                "company-1", "Bond", null,
                new BigDecimal("70"), new BigDecimal("100"), "cm",
                false, true, null, null, null, true,
                LocalDate.of(2026, 10, 6), LocalDateTime.of(2026, 10, 6, 12, 0)
        ));
    }

    @Test
    void createDefaultsMinRemnantUnitToPaperUnitWhenOmitted() {
        Paper paper = Paper.createNew(
                "company-1", "Bond", null,
                new BigDecimal("70"), new BigDecimal("100"), "cm",
                false, true, new BigDecimal("20"), new BigDecimal("20"), null, true,
                LocalDate.of(2026, 10, 6), LocalDateTime.of(2026, 10, 6, 12, 0)
        );
        assertTrue(paper.isAcceptsRemnants());
        assertEquals("cm", paper.getMinRemnantUnit());
    }

    @Test
    void createStoresExplicitMinRemnantUnit() {
        Paper paper = Paper.createNew(
                "company-1", "Bond", null,
                new BigDecimal("70"), new BigDecimal("100"), "cm",
                false, true, new BigDecimal("200"), new BigDecimal("200"), "mm", true,
                LocalDate.of(2026, 10, 6), LocalDateTime.of(2026, 10, 6, 12, 0)
        );
        assertEquals("mm", paper.getMinRemnantUnit());
        assertEquals(new BigDecimal("200.00"), paper.getMinRemnantWidth());
    }

    @Test
    void createClearsMinsWhenNotAccepting() {
        Paper paper = Paper.createNew(
                "company-1", "Bond", null,
                new BigDecimal("70"), new BigDecimal("100"), "cm",
                false, false, null, null, null, true,
                LocalDate.of(2026, 10, 6), LocalDateTime.of(2026, 10, 6, 12, 0)
        );
        assertFalse(paper.isAcceptsRemnants());
        assertEquals(null, paper.getMinRemnantWidth());
        assertEquals(null, paper.getMinRemnantUnit());
    }

    @Test
    void updateCanDisableRemnantsAndClearMins() {
        Paper paper = Paper.createNew(
                "company-1",
                "Bond 75",
                new BigDecimal("75"),
                new BigDecimal("70"),
                new BigDecimal("100"),
                "cm",
                false,
                true,
                new BigDecimal("20"),
                new BigDecimal("25"),
                "cm",
                true,
                LocalDate.of(2026, 10, 6),
                LocalDateTime.of(2026, 10, 6, 12, 0)
        );
        assertTrue(paper.isAcceptsRemnants());

        Paper closed = paper.update(
                paper.getName(), paper.getGrammage(), paper.getWidth(), paper.getHeight(),
                paper.getUnit(), paper.isCoated(), false, null, null, null, true,
                LocalDateTime.of(2026, 10, 6, 13, 0)
        );
        assertFalse(closed.isAcceptsRemnants());
        assertEquals(null, closed.getMinRemnantWidth());
        assertEquals(null, closed.getMinRemnantHeight());
        assertEquals(null, closed.getMinRemnantUnit());
    }
}
