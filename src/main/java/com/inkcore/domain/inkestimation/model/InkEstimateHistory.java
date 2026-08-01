package com.inkcore.domain.inkestimation.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class InkEstimateHistory {

    private final String inkEstimateHistoryId;
    private final String originalFileName;
    private final long originalSizeBytes;
    private final String mimeType;
    private final double widthCm;
    private final double heightCm;
    private final int sheetCount;
    private final int dpi;
    private final double gramsPerCm2;
    private final double processGramsOrder;
    private final double spotGramsOrder;
    private final double totalGramsOrder;
    private final int spotCount;
    private final String iccProfileUsed;
    private final Instant estimateDate;
    private final long durationMs;
    private final String userId;

    private InkEstimateHistory(
            String inkEstimateHistoryId,
            String originalFileName,
            long originalSizeBytes,
            String mimeType,
            double widthCm,
            double heightCm,
            int sheetCount,
            int dpi,
            double gramsPerCm2,
            double processGramsOrder,
            double spotGramsOrder,
            double totalGramsOrder,
            int spotCount,
            String iccProfileUsed,
            Instant estimateDate,
            long durationMs,
            String userId
    ) {
        this.inkEstimateHistoryId = inkEstimateHistoryId;
        this.originalFileName = originalFileName;
        this.originalSizeBytes = originalSizeBytes;
        this.mimeType = mimeType;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.sheetCount = sheetCount;
        this.dpi = dpi;
        this.gramsPerCm2 = gramsPerCm2;
        this.processGramsOrder = processGramsOrder;
        this.spotGramsOrder = spotGramsOrder;
        this.totalGramsOrder = totalGramsOrder;
        this.spotCount = spotCount;
        this.iccProfileUsed = iccProfileUsed;
        this.estimateDate = estimateDate;
        this.durationMs = durationMs;
        this.userId = userId;
    }

    public static InkEstimateHistory fromResult(InkEstimateResult result, String userId, Instant when) {
        return new InkEstimateHistory(
                UUID.randomUUID().toString(),
                result.getOriginalFileName(),
                result.getOriginalSizeBytes(),
                result.getContentType(),
                result.getWidthCm(),
                result.getHeightCm(),
                result.getSheetCount(),
                result.getDpi(),
                result.getGramsPerCm2AtFullCoverage(),
                result.getProcessGramsOrder(),
                result.getSpotGramsOrder(),
                result.getTotalGramsOrder(),
                result.getSpotInks().size(),
                result.getIccProfileUsed(),
                Objects.requireNonNull(when),
                result.getProcessingTimeMs(),
                userId
        );
    }

    public static InkEstimateHistory reconstitute(
            String id,
            String originalFileName,
            long originalSizeBytes,
            String mimeType,
            double widthCm,
            double heightCm,
            int sheetCount,
            int dpi,
            double gramsPerCm2,
            double processGramsOrder,
            double spotGramsOrder,
            double totalGramsOrder,
            int spotCount,
            String iccProfileUsed,
            Instant estimateDate,
            long durationMs,
            String userId
    ) {
        return new InkEstimateHistory(
                id, originalFileName, originalSizeBytes, mimeType, widthCm, heightCm, sheetCount, dpi,
                gramsPerCm2, processGramsOrder, spotGramsOrder, totalGramsOrder, spotCount, iccProfileUsed,
                estimateDate, durationMs, userId
        );
    }

    public String getInkEstimateHistoryId() {
        return inkEstimateHistoryId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public long getOriginalSizeBytes() {
        return originalSizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public double getWidthCm() {
        return widthCm;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public int getSheetCount() {
        return sheetCount;
    }

    public int getDpi() {
        return dpi;
    }

    public double getGramsPerCm2() {
        return gramsPerCm2;
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

    public int getSpotCount() {
        return spotCount;
    }

    public String getIccProfileUsed() {
        return iccProfileUsed;
    }

    public Instant getEstimateDate() {
        return estimateDate;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getUserId() {
        return userId;
    }
}
