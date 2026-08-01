package com.inkcore.domain.inkestimation.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InkConsumptionCalculatorTest {

    @Test
    void grams_formula_coverageTimesAreaTimesFactor() {
        // 50% cobertura, 7000 cm², 0.00021 g/cm² → 0.735 g
        assertEquals(0.735, InkConsumptionCalculator.grams(50.0, 7000.0, 0.00021), 0.000001);
    }

    @Test
    void build_aggregatesProcessAndOrderTotals_uniformDensity() {
        InkEstimateRequest request = new InkEstimateRequest(
                new byte[]{1, 2, 3},
                "a.png",
                "image/png",
                70,
                100,
                1000,
                300,
                0.00021,
                "user",
                "FOGRA39.icc"
        );
        InkCoverageAnalysis analysis = new InkCoverageAnalysis(
                100,
                100,
                300,
                "FOGRA39.icc",
                List.of(
                        new InkCoverageAnalysis.RawInkCoverage("Cian", "C", 10.0, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Magenta", "M", 0.0, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Amarillo", "Y", 0.0, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Negro", "K", 5.0, null, true)
                ),
                List.of(
                        new InkCoverageAnalysis.RawInkCoverage("PANTONE 185 C", "SPOT", 2.0, "#E4002B", true)
                )
        );

        InkEstimateResult result = InkConsumptionCalculator.build(
                request, analysis, InkDensityFactors.uniform(0.00021), 12
        );
        // process per sheet: (10+5)/100 * 7000 * 0.00021 = 0.2205
        assertEquals(0.2205, result.getProcessGramsPerSheet(), 0.000001);
        // spot: 2/100 * 7000 * 0.00021 = 0.0294
        assertEquals(0.0294, result.getSpotGramsPerSheet(), 0.000001);
        assertEquals(0.2499, result.getTotalGramsPerSheet(), 0.000001);
        assertEquals(249.9, result.getTotalGramsOrder(), 0.001);
        assertEquals(1, result.getSpotInks().size());
        assertTrue(result.getSpotInks().get(0).isCoverageMeasured());
    }

    @Test
    void build_multiPageAnalysis_sumsCoverageIntoGrams() {
        // Simula 2 páginas: cobertura proceso ya sumada (10+10=20 en C)
        InkEstimateRequest request = new InkEstimateRequest(
                new byte[]{1},
                "a.pdf",
                "application/pdf",
                70,
                100,
                1,
                300,
                0.00021,
                "user",
                "FOGRA39.icc",
                List.of(1, 2)
        );
        InkCoverageAnalysis analysis = new InkCoverageAnalysis(
                100, 100, 300, "FOGRA39.icc",
                List.of(
                        new InkCoverageAnalysis.RawInkCoverage("Cian", "C", 20.0, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Magenta", "M", 0.0, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Amarillo", "Y", 0.0, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Negro", "K", 0.0, null, true)
                ),
                List.of(),
                List.of(1, 2),
                true,
                false,
                List.of()
        );
        InkEstimateResult result = InkConsumptionCalculator.build(
                request, analysis, InkDensityFactors.uniform(0.00021), 1
        );
        // 20% * 7000 * 0.00021 = 0.294
        assertEquals(0.294, result.getProcessGramsPerSheet(), 0.000001);
        assertEquals(0.0, result.getSpotGramsPerSheet(), 0.000001);
        assertTrue(result.getSpotInks().isEmpty());
        assertTrue(result.getDeclaredSpotColorNames().isEmpty());
    }

    @Test
    void build_usesPerChannelDensityFactors() {
        InkEstimateRequest request = new InkEstimateRequest(
                new byte[]{1},
                "a.png",
                "image/png",
                100,
                100,
                1,
                300,
                null,
                "user",
                null
        );
        // 100% cyan only, area 10000 cm², factor C=0.0002 → 2.0 g
        InkCoverageAnalysis analysis = new InkCoverageAnalysis(
                10, 10, 300, "FOGRA39.icc",
                List.of(
                        new InkCoverageAnalysis.RawInkCoverage("Cian", "C", 100.0, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Magenta", "M", 0.0, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Amarillo", "Y", 0.0, null, true),
                        new InkCoverageAnalysis.RawInkCoverage("Negro", "K", 0.0, null, true)
                ),
                List.of()
        );
        InkDensityFactors density = new InkDensityFactors(0.0002, 0.00021, 0.00020, 0.00022, 0.00025);
        InkEstimateResult result = InkConsumptionCalculator.build(request, analysis, density, 1);
        assertEquals(2.0, result.getProcessGramsPerSheet(), 0.000001);
        assertEquals(0.0002, result.getGramsPerCm2AtFullCoverage(), 0.0000001);
    }
}
