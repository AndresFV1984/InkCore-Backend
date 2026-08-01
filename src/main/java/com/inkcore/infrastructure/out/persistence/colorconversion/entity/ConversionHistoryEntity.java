package com.inkcore.infrastructure.out.persistence.colorconversion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Entity
@Table(name = "conversion_history", schema = "indicolors")
public class ConversionHistoryEntity implements Persistable<String> {

    @Id
    @Column(name = "conversion_history_id", length = 64)
    private String conversionHistoryId;

    @Transient
    private boolean isNew = true;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "original_size_bytes", nullable = false)
    private long originalSizeBytes;

    @Column(name = "final_size_bytes", nullable = false)
    private long finalSizeBytes;

    @Column(name = "rendering_intent", nullable = false, length = 40)
    private String renderingIntent;

    @Column(name = "icc_profile_used", length = 120)
    private String iccProfileUsed;

    @Column(name = "conversion_date", nullable = false)
    private Instant conversionDate;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    public ConversionHistoryEntity() {
    }

    @Override
    public String getId() {
        return conversionHistoryId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public String getConversionHistoryId() {
        return conversionHistoryId;
    }

    public void setConversionHistoryId(String conversionHistoryId) {
        this.conversionHistoryId = conversionHistoryId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public long getOriginalSizeBytes() {
        return originalSizeBytes;
    }

    public void setOriginalSizeBytes(long originalSizeBytes) {
        this.originalSizeBytes = originalSizeBytes;
    }

    public long getFinalSizeBytes() {
        return finalSizeBytes;
    }

    public void setFinalSizeBytes(long finalSizeBytes) {
        this.finalSizeBytes = finalSizeBytes;
    }

    public String getRenderingIntent() {
        return renderingIntent;
    }

    public void setRenderingIntent(String renderingIntent) {
        this.renderingIntent = renderingIntent;
    }

    public String getIccProfileUsed() {
        return iccProfileUsed;
    }

    public void setIccProfileUsed(String iccProfileUsed) {
        this.iccProfileUsed = iccProfileUsed;
    }

    public Instant getConversionDate() {
        return conversionDate;
    }

    public void setConversionDate(Instant conversionDate) {
        this.conversionDate = conversionDate;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
