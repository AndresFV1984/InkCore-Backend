package com.inkcore.domain.inkestimation.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InkPageSelectionTest {

    @Test
    void parse_emptyMeansAll() {
        assertTrue(InkPageSelection.parse(null).isEmpty());
        assertTrue(InkPageSelection.parse("  ").isEmpty());
    }

    @Test
    void parse_singleAndPair() {
        assertEquals(List.of(1), InkPageSelection.parse("1"));
        assertEquals(List.of(1, 2), InkPageSelection.parse("1,2"));
        assertEquals(List.of(3, 5), InkPageSelection.parse("3;5"));
    }

    @Test
    void parse_rejectsMoreThanTwo() {
        assertThrows(IllegalArgumentException.class, () -> InkPageSelection.parse("1,2,3"));
    }

    @Test
    void parse_rejectsZeroBased() {
        assertThrows(IllegalArgumentException.class, () -> InkPageSelection.parse("0,1"));
    }

    @Test
    void validateAgainstDocument_rejectsOutOfRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> InkPageSelection.validateAgainstDocument(List.of(1, 5), 3)
        );
    }

    @Test
    void includes_emptySelectsAll() {
        assertTrue(InkPageSelection.includes(List.of(), 99));
        assertTrue(InkPageSelection.includes(List.of(2), 2));
        assertTrue(!InkPageSelection.includes(List.of(2), 1));
    }
}
