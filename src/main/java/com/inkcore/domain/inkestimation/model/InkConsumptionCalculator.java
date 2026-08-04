package com.inkcore.domain.inkestimation.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Aplica: gramos = (cobertura/100) × área_cm² × factor_g/cm²_canal × pliegos.
 */
public final class InkConsumptionCalculator {

    private InkConsumptionCalculator() {
    }

    public static InkEstimateResult build(
            InkEstimateRequest request,
            InkCoverageAnalysis analysis,
            InkDensityFactors density,
            long processingTimeMs
    ) {
        double area = request.areaCm2();
        int sheets = request.getSheetCount();

        List<InkCoverage> process = new ArrayList<>();
        double processPerSheet = 0;
        for (InkCoverageAnalysis.RawInkCoverage raw : analysis.processInks()) {
            double factor = density.forChannel(raw.channel());
            double gSheet = grams(raw.coveragePercent(), area, factor);
            double gOrder = gSheet * sheets;
            processPerSheet += gSheet;
            process.add(InkCoverage.process(raw.name(), raw.channel(), raw.coveragePercent(), gSheet, gOrder));
        }

        List<InkCoverage> spots = new ArrayList<>();
        double spotPerSheet = 0;
        for (InkCoverageAnalysis.RawInkCoverage raw : analysis.spotInks()) {
            double factor = density.forChannel("SPOT");
            double gSheet = raw.coverageMeasured() ? grams(raw.coveragePercent(), area, factor) : 0.0;
            double gOrder = gSheet * sheets;
            spotPerSheet += gSheet;
            spots.add(InkCoverage.spot(
                    raw.name(),
                    raw.coveragePercent(),
                    raw.swatchHex(),
                    raw.coverageMeasured(),
                    gSheet,
                    gOrder
            ));
        }

        double totalPerSheet = processPerSheet + spotPerSheet;
        return new InkEstimateResult(
                request.getOriginalFileName(),
                request.getMimeType(),
                request.getWidthCm(),
                request.getHeightCm(),
                area,
                sheets,
                analysis.dpiUsed(),
                density.reference(),
                analysis.widthPx(),
                analysis.heightPx(),
                analysis.iccProfileUsed(),
                process,
                spots,
                processPerSheet,
                spotPerSheet,
                totalPerSheet,
                processPerSheet * sheets,
                spotPerSheet * sheets,
                totalPerSheet * sheets,
                processingTimeMs,
                request.getOriginalSizeBytes(),
                analysis.pagesAnalyzed(),
                analysis.spotInventoryVerified(),
                analysis.hasSpotColors(),
                analysis.declaredSpotColorNames(),
                analysis.colorEngine()
        );
    }

    static double grams(double coveragePercent, double areaCm2, double gramsPerCm2) {
        return (coveragePercent / 100.0) * areaCm2 * gramsPerCm2;
    }
}
