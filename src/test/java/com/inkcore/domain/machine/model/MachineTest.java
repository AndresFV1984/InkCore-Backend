package com.inkcore.domain.machine.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineTest {

    @Test
    void createNewValidatesInputsAndLeavesCostPerHourToTheDatabase() {
        Machine machine = Machine.createNew(
                "company-1",
                "Offset",
                MachineType.IMPRESION,
                null,
                null,
                new BigDecimal("100000"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("5"),
                new BigDecimal("1600"),
                15,
                true,
                true,
                LocalDate.of(2026, 9, 24),
                LocalDateTime.of(2026, 9, 24, 10, 0)
        );
        assertEquals(MachineType.IMPRESION, machine.getMachineType());
        assertNull(machine.getCostPerHour());
        assertEquals(15, machine.getSetupMinutes());
        assertTrue(machine.isPreferred());
    }

    @Test
    void rejectsNonPositiveUsefulLife() {
        assertThrows(IllegalArgumentException.class, () -> Machine.createNew(
                "company-1",
                "Offset",
                MachineType.IMPRESION,
                null,
                null,
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1600"),
                0,
                false,
                true,
                LocalDate.of(2026, 9, 24),
                LocalDateTime.of(2026, 9, 24, 10, 0)
        ));
    }

    @Test
    void rejectsNegativeSetupMinutes() {
        assertThrows(IllegalArgumentException.class, () -> Machine.createNew(
                "company-1",
                "Offset",
                MachineType.IMPRESION,
                null,
                null,
                new BigDecimal("100000"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1600"),
                -1,
                false,
                true,
                LocalDate.of(2026, 9, 24),
                LocalDateTime.of(2026, 9, 24, 10, 0)
        ));
    }

    @Test
    void nullSetupMinutesDefaultsToZero() {
        Machine machine = Machine.createNew(
                "company-1",
                "Offset",
                MachineType.IMPRESION,
                null,
                null,
                new BigDecimal("100000"),
                new BigDecimal("10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1600"),
                null,
                false,
                true,
                LocalDate.of(2026, 9, 24),
                LocalDateTime.of(2026, 9, 24, 10, 0)
        );
        assertEquals(0, machine.getSetupMinutes());
        assertFalse(machine.isPreferred());
    }
}
