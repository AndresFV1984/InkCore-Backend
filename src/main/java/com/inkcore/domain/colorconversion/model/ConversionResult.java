package com.inkcore.domain.colorconversion.model;

import java.util.Objects;

public final class ConversionResult {

    private final byte[] convertedBytes;
    private final String outputFileName;
    private final String outputMimeType;
    private final long originalSizeBytes;
    private final long finalSizeBytes;
    private final long processingTimeMs;
    private final int widthPx;
    private final int heightPx;
    private final RenderingIntent renderingIntent;
    private final String iccProfileUsed;
    private final float brightnessLift;
    private final float vibranceBoost;
    private final boolean softProofBrightnessMatch;
    private final QualityPreset qualityPreset;
    private final byte[] previewRgbBytes;
    private final String previewMimeType;
    /** luma(soft-proof CMYK→RGB) / luma(RGB original). Null si no se midió. */
    private final Double softProofLumaRatio;

    public ConversionResult(
            byte[] convertedBytes,
            String outputFileName,
            String outputMimeType,
            long originalSizeBytes,
            long finalSizeBytes,
            long processingTimeMs,
            int widthPx,
            int heightPx,
            RenderingIntent renderingIntent,
            String iccProfileUsed
    ) {
        this(
                convertedBytes,
                outputFileName,
                outputMimeType,
                originalSizeBytes,
                finalSizeBytes,
                processingTimeMs,
                widthPx,
                heightPx,
                renderingIntent,
                iccProfileUsed,
                0f,
                0f,
                false,
                null,
                null,
                null,
                null
        );
    }

    public ConversionResult(
            byte[] convertedBytes,
            String outputFileName,
            String outputMimeType,
            long originalSizeBytes,
            long finalSizeBytes,
            long processingTimeMs,
            int widthPx,
            int heightPx,
            RenderingIntent renderingIntent,
            String iccProfileUsed,
            float brightnessLift,
            float vibranceBoost,
            boolean softProofBrightnessMatch
    ) {
        this(
                convertedBytes,
                outputFileName,
                outputMimeType,
                originalSizeBytes,
                finalSizeBytes,
                processingTimeMs,
                widthPx,
                heightPx,
                renderingIntent,
                iccProfileUsed,
                brightnessLift,
                vibranceBoost,
                softProofBrightnessMatch,
                null,
                null,
                null,
                null
        );
    }

    public ConversionResult(
            byte[] convertedBytes,
            String outputFileName,
            String outputMimeType,
            long originalSizeBytes,
            long finalSizeBytes,
            long processingTimeMs,
            int widthPx,
            int heightPx,
            RenderingIntent renderingIntent,
            String iccProfileUsed,
            float brightnessLift,
            float vibranceBoost,
            boolean softProofBrightnessMatch,
            QualityPreset qualityPreset,
            byte[] previewRgbBytes,
            String previewMimeType
    ) {
        this(
                convertedBytes,
                outputFileName,
                outputMimeType,
                originalSizeBytes,
                finalSizeBytes,
                processingTimeMs,
                widthPx,
                heightPx,
                renderingIntent,
                iccProfileUsed,
                brightnessLift,
                vibranceBoost,
                softProofBrightnessMatch,
                qualityPreset,
                previewRgbBytes,
                previewMimeType,
                null
        );
    }

    public ConversionResult(
            byte[] convertedBytes,
            String outputFileName,
            String outputMimeType,
            long originalSizeBytes,
            long finalSizeBytes,
            long processingTimeMs,
            int widthPx,
            int heightPx,
            RenderingIntent renderingIntent,
            String iccProfileUsed,
            float brightnessLift,
            float vibranceBoost,
            boolean softProofBrightnessMatch,
            QualityPreset qualityPreset,
            byte[] previewRgbBytes,
            String previewMimeType,
            Double softProofLumaRatio
    ) {
        this.convertedBytes = Objects.requireNonNull(convertedBytes, "convertedBytes");
        this.outputFileName = Objects.requireNonNull(outputFileName, "outputFileName");
        this.outputMimeType = Objects.requireNonNull(outputMimeType, "outputMimeType");
        this.originalSizeBytes = originalSizeBytes;
        this.finalSizeBytes = finalSizeBytes;
        this.processingTimeMs = processingTimeMs;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.renderingIntent = renderingIntent;
        this.iccProfileUsed = iccProfileUsed;
        this.brightnessLift = brightnessLift;
        this.vibranceBoost = vibranceBoost;
        this.softProofBrightnessMatch = softProofBrightnessMatch;
        this.qualityPreset = qualityPreset;
        this.previewRgbBytes = previewRgbBytes;
        this.previewMimeType = previewMimeType;
        this.softProofLumaRatio = softProofLumaRatio;
    }

    public byte[] getConvertedBytes() {
        return convertedBytes;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public String getOutputMimeType() {
        return outputMimeType;
    }

    public long getOriginalSizeBytes() {
        return originalSizeBytes;
    }

    public long getFinalSizeBytes() {
        return finalSizeBytes;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public int getWidthPx() {
        return widthPx;
    }

    public int getHeightPx() {
        return heightPx;
    }

    public RenderingIntent getRenderingIntent() {
        return renderingIntent;
    }

    public String getIccProfileUsed() {
        return iccProfileUsed;
    }

    public float getBrightnessLift() {
        return brightnessLift;
    }

    public float getVibranceBoost() {
        return vibranceBoost;
    }

    public boolean isSoftProofBrightnessMatch() {
        return softProofBrightnessMatch;
    }

    public QualityPreset getQualityPreset() {
        return qualityPreset;
    }

    public byte[] getPreviewRgbBytes() {
        return previewRgbBytes;
    }

    public String getPreviewMimeType() {
        return previewMimeType;
    }

    public Double getSoftProofLumaRatio() {
        return softProofLumaRatio;
    }
}
