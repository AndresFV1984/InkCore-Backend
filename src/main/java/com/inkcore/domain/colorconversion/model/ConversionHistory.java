package com.inkcore.domain.colorconversion.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ConversionHistory {

    private final String conversionHistoryId;
    private final String originalFileName;
    private final long originalSizeBytes;
    private final long finalSizeBytes;
    private final RenderingIntent renderingIntent;
    private final String iccProfileUsed;
    private final Instant conversionDate;
    private final long durationMs;
    private final String userId;
    private final String mimeType;

    private ConversionHistory(
            String conversionHistoryId,
            String originalFileName,
            long originalSizeBytes,
            long finalSizeBytes,
            RenderingIntent renderingIntent,
            String iccProfileUsed,
            Instant conversionDate,
            long durationMs,
            String userId,
            String mimeType
    ) {
        this.conversionHistoryId = conversionHistoryId;
        this.originalFileName = originalFileName;
        this.originalSizeBytes = originalSizeBytes;
        this.finalSizeBytes = finalSizeBytes;
        this.renderingIntent = renderingIntent;
        this.iccProfileUsed = iccProfileUsed;
        this.conversionDate = conversionDate;
        this.durationMs = durationMs;
        this.userId = userId;
        this.mimeType = mimeType;
    }

    public static ConversionHistory createNew(
            String originalFileName,
            long originalSizeBytes,
            long finalSizeBytes,
            RenderingIntent renderingIntent,
            String iccProfileUsed,
            Instant conversionDate,
            long durationMs,
            String userId,
            String mimeType
    ) {
        return new ConversionHistory(
                UUID.randomUUID().toString(),
                Objects.requireNonNull(originalFileName, "originalFileName"),
                originalSizeBytes,
                finalSizeBytes,
                Objects.requireNonNull(renderingIntent, "renderingIntent"),
                iccProfileUsed,
                Objects.requireNonNull(conversionDate, "conversionDate"),
                durationMs,
                userId,
                mimeType
        );
    }

    public static ConversionHistory reconstitute(
            String conversionHistoryId,
            String originalFileName,
            long originalSizeBytes,
            long finalSizeBytes,
            RenderingIntent renderingIntent,
            String iccProfileUsed,
            Instant conversionDate,
            long durationMs,
            String userId,
            String mimeType
    ) {
        return new ConversionHistory(
                conversionHistoryId,
                originalFileName,
                originalSizeBytes,
                finalSizeBytes,
                renderingIntent,
                iccProfileUsed,
                conversionDate,
                durationMs,
                userId,
                mimeType
        );
    }

    public String getConversionHistoryId() {
        return conversionHistoryId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public long getOriginalSizeBytes() {
        return originalSizeBytes;
    }

    public long getFinalSizeBytes() {
        return finalSizeBytes;
    }

    public RenderingIntent getRenderingIntent() {
        return renderingIntent;
    }

    public String getIccProfileUsed() {
        return iccProfileUsed;
    }

    public Instant getConversionDate() {
        return conversionDate;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getUserId() {
        return userId;
    }

    public String getMimeType() {
        return mimeType;
    }
}
