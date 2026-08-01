package com.inkcore.domain.colorconversion.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DestinationIccProfileTest {

    @Test
    void resolvesIsoCoatedAliasToFogra39() {
        assertEquals("FOGRA39.icc", DestinationIccProfile.resolveClasspathFileName("ISOcoated_v2_eci.icc"));
        assertEquals("FOGRA39.icc", DestinationIccProfile.resolveClasspathFileName("fogra39"));
        assertTrue(DestinationIccProfile.findByNameOrAlias("CoatedFOGRA39.icc").isPresent());
    }

    @Test
    void openApiValues_includeMajorPrintConditions() {
        String[] values = DestinationIccProfile.openApiAllowableValues();
        assertTrue(values.length >= 7);
        assertTrue(java.util.Arrays.asList(values).contains("FOGRA39.icc"));
        assertTrue(java.util.Arrays.asList(values).contains("FOGRA51.icc"));
        assertTrue(java.util.Arrays.asList(values).contains("GRACoL2013.icc"));
        assertTrue(java.util.Arrays.asList(values).contains("SWOP2006_Coated3v2.icc"));
    }
}
