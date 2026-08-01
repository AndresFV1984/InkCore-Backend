package com.inkcore.domain.inkestimation.model;

/**
 * Cobertura y consumo de una tinta de proceso (C/M/Y/K) o spot.
 */
public final class InkCoverage {

    private final String name;
    private final String channel;
    private final double coveragePercent;
    private final String swatchHex;
    private final boolean coverageMeasured;
    private final double gramsPerSheet;
    private final double gramsOrder;

    public InkCoverage(
            String name,
            String channel,
            double coveragePercent,
            String swatchHex,
            boolean coverageMeasured,
            double gramsPerSheet,
            double gramsOrder
    ) {
        this.name = name;
        this.channel = channel;
        this.coveragePercent = coveragePercent;
        this.swatchHex = swatchHex;
        this.coverageMeasured = coverageMeasured;
        this.gramsPerSheet = gramsPerSheet;
        this.gramsOrder = gramsOrder;
    }

    public static InkCoverage process(
            String name,
            String channel,
            double coveragePercent,
            double gramsPerSheet,
            double gramsOrder
    ) {
        return new InkCoverage(name, channel, coveragePercent, null, true, gramsPerSheet, gramsOrder);
    }

    public static InkCoverage spot(
            String name,
            double coveragePercent,
            String swatchHex,
            boolean coverageMeasured,
            double gramsPerSheet,
            double gramsOrder
    ) {
        return new InkCoverage(name, "SPOT", coveragePercent, swatchHex, coverageMeasured, gramsPerSheet, gramsOrder);
    }

    public String getName() {
        return name;
    }

    public String getChannel() {
        return channel;
    }

    public double getCoveragePercent() {
        return coveragePercent;
    }

    public String getSwatchHex() {
        return swatchHex;
    }

    public boolean isCoverageMeasured() {
        return coverageMeasured;
    }

    public double getGramsPerSheet() {
        return gramsPerSheet;
    }

    public double getGramsOrder() {
        return gramsOrder;
    }
}
