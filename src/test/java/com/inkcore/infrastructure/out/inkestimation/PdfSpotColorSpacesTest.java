package com.inkcore.infrastructure.out.inkestimation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfSpotColorSpacesTest {

    @Test
    void separationLiteralName_isNeverAColorant() {
        // PDSeparation.getName() == "Separation"; no debe usarse como tinta
        assertFalse(SpotColorantNames.isReportableSpotColorant("Separation"));
        assertNull(PdfSpotColorSpaces.separationColorantName(null));
    }

    @Test
    void pantoneNames_remainReportable() {
        assertTrue(SpotColorantNames.isReportableSpotColorant("PANTONE 2925 C"));
        assertTrue(SpotColorantNames.isReportableSpotColorant("PANTONE Medium Blue C"));
        assertTrue(SpotColorantNames.looksLikePantone("PANTONE 2915 C"));
    }
}
