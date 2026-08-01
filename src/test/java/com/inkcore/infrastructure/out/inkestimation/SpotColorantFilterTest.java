package com.inkcore.infrastructure.out.inkestimation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpotColorantFilterTest {

    @Test
    void specialPdf_All_and_None_areNotReportableSpots() {
        assertFalse(SpotColorantNames.isReportableSpotColorant("All"));
        assertFalse(SpotColorantNames.isReportableSpotColorant("None"));
        assertTrue(SpotColorantNames.isSpecialPdfColorant("All"));
        assertNull(SpotColorantNames.exactSpotReference("All"));
        assertNull(SpotColorantNames.exactSpotReference("Separation"));
    }

    @Test
    void processNames_areNotReportableSpots() {
        assertFalse(SpotColorantNames.isReportableSpotColorant("Black"));
        assertFalse(SpotColorantNames.isReportableSpotColorant("Cyan"));
        assertNull(SpotColorantNames.exactSpotReference("Cyan"));
    }

    @Test
    void technicalMarks_areNotReportableSpots() {
        assertFalse(SpotColorantNames.isReportableSpotColorant("CutContour"));
        assertFalse(SpotColorantNames.isReportableSpotColorant("Die Line"));
        assertFalse(SpotColorantNames.isReportableSpotColorant("Varnish"));
        assertFalse(SpotColorantNames.isReportableSpotColorant("Registration"));
    }

    @Test
    void realPantone_keepsExactReferenceName() {
        assertEquals("PANTONE 185 C", SpotColorantNames.exactSpotReference("PANTONE 185 C"));
        assertEquals("PANTONE Medium Blue C", SpotColorantNames.exactSpotReference("  PANTONE Medium Blue C  "));
        assertEquals("PANTONE 2925 C", SpotColorantNames.exactSpotReference("PANTONE 2925 C"));
        assertTrue(SpotColorantNames.looksLikePantone("PANTONE 185 C"));
        // No reescribe ni antepone "Pantone "
        assertEquals("Logo Spot Blue", SpotColorantNames.exactSpotReference("Logo Spot Blue"));
    }
}
