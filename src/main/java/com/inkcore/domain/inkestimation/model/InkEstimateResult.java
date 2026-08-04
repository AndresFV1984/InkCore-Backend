package com.inkcore.domain.inkestimation.model;

import java.util.List;
import java.util.Objects;

public final class InkEstimateResult {

    private final String originalFileName;
    private final String contentType;
    private final double widthCm;
    private final double heightCm;
    private final double areaCm2;
    private final int sheetCount;
    private final int dpi;
    private final double gramsPerCm2AtFullCoverage;
    private final int widthPx;
    private final int heightPx;
    private final String iccProfileUsed;
    private final List<InkCoverage> processInks;
    private final List<InkCoverage> spotInks;
    private final double processGramsPerSheet;
    private final double spotGramsPerSheet;
    private final double totalGramsPerSheet;
    private final double processGramsOrder;
    private final double spotGramsOrder;
    private final double totalGramsOrder;
    private final long processingTimeMs;
    private final long originalSizeBytes;
    private final List<Integer> pagesAnalyzed;
    private final boolean spotInventoryVerified;
    private final boolean hasSpotColors;
    private final List<String> declaredSpotColorNames;
    private final String colorEngine;

    public InkEstimateResult(
            String originalFileName,
            String contentType,
            double widthCm,
            double heightCm,
            double areaCm2,
            int sheetCount,
            int dpi,
            double gramsPerCm2AtFullCoverage,
            int widthPx,
            int heightPx,
            String iccProfileUsed,
            List<InkCoverage> processInks,
            List<InkCoverage> spotInks,
            double processGramsPerSheet,
            double spotGramsPerSheet,
            double totalGramsPerSheet,
            double processGramsOrder,
            double spotGramsOrder,
            double totalGramsOrder,
            long processingTimeMs,
            long originalSizeBytes,
            List<Integer> pagesAnalyzed,
            boolean spotInventoryVerified,
            boolean hasSpotColors,
            List<String> declaredSpotColorNames,
            String colorEngine
    ) {
        this.originalFileName = Objects.requireNonNull(originalFileName);
        this.contentType = contentType;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.areaCm2 = areaCm2;
        this.sheetCount = sheetCount;
        this.dpi = dpi;
        this.gramsPerCm2AtFullCoverage = gramsPerCm2AtFullCoverage;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.iccProfileUsed = iccProfileUsed;
        this.processInks = List.copyOf(processInks);
        this.spotInks = List.copyOf(spotInks);
        this.processGramsPerSheet = processGramsPerSheet;
        this.spotGramsPerSheet = spotGramsPerSheet;
        this.totalGramsPerSheet = totalGramsPerSheet;
        this.processGramsOrder = processGramsOrder;
        this.spotGramsOrder = spotGramsOrder;
        this.totalGramsOrder = totalGramsOrder;
        this.processingTimeMs = processingTimeMs;
        this.originalSizeBytes = originalSizeBytes;
        this.pagesAnalyzed = pagesAnalyzed == null ? List.of() : List.copyOf(pagesAnalyzed);
        this.spotInventoryVerified = spotInventoryVerified;
        this.hasSpotColors = hasSpotColors;
        this.declaredSpotColorNames = declaredSpotColorNames == null
                ? List.of()
                : List.copyOf(declaredSpotColorNames);
        this.colorEngine = colorEngine == null || colorEngine.isBlank() ? "littlecms" : colorEngine;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public double getWidthCm() {
        return widthCm;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public double getAreaCm2() {
        return areaCm2;
    }

    public int getSheetCount() {
        return sheetCount;
    }

    public int getDpi() {
        return dpi;
    }

    public double getGramsPerCm2AtFullCoverage() {
        return gramsPerCm2AtFullCoverage;
    }

    public int getWidthPx() {
        return widthPx;
    }

    public int getHeightPx() {
        return heightPx;
    }

    public String getIccProfileUsed() {
        return iccProfileUsed;
    }

    public List<InkCoverage> getProcessInks() {
        return processInks;
    }

    public List<InkCoverage> getSpotInks() {
        return spotInks;
    }

    public double getProcessGramsPerSheet() {
        return processGramsPerSheet;
    }

    public double getSpotGramsPerSheet() {
        return spotGramsPerSheet;
    }

    public double getTotalGramsPerSheet() {
        return totalGramsPerSheet;
    }

    public double getProcessGramsOrder() {
        return processGramsOrder;
    }

    public double getSpotGramsOrder() {
        return spotGramsOrder;
    }

    public double getTotalGramsOrder() {
        return totalGramsOrder;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public long getOriginalSizeBytes() {
        return originalSizeBytes;
    }

    /** Páginas PDF analizadas (1-based). Vacía si raster o no aplica. */
    public List<Integer> getPagesAnalyzed() {
        return pagesAnalyzed;
    }

    public boolean isSpotInventoryVerified() {
        return spotInventoryVerified;
    }

    public boolean hasSpotColors() {
        return hasSpotColors;
    }

    /**
     * Nombres Separation/DeviceN declarados en el PDF (pueden no tener consumo medido).
     */
    public List<String> getDeclaredSpotColorNames() {
        return declaredSpotColorNames;
    }

    /**
     * Motor de color RGB→CMYK: {@code littlecms} (obligatorio en respuestas OK).
     */
    public String getColorEngine() {
        return colorEngine;
    }
}
