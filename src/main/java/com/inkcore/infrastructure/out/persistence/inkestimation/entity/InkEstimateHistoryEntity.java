package com.inkcore.infrastructure.out.persistence.inkestimation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ink_estimate_history", schema = "indicolors")
public class InkEstimateHistoryEntity implements Persistable<String> {

    @Id
    @Column(name = "ink_estimate_history_id", length = 64)
    private String inkEstimateHistoryId;

    @Transient
    private boolean isNew = true;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "original_size_bytes", nullable = false)
    private long originalSizeBytes;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "width_cm", nullable = false, precision = 12, scale = 4)
    private BigDecimal widthCm;

    @Column(name = "height_cm", nullable = false, precision = 12, scale = 4)
    private BigDecimal heightCm;

    @Column(name = "sheet_count", nullable = false)
    private int sheetCount;

    @Column(name = "dpi", nullable = false)
    private int dpi;

    @Column(name = "grams_per_cm2", nullable = false, precision = 16, scale = 10)
    private BigDecimal gramsPerCm2;

    @Column(name = "process_grams_order", nullable = false, precision = 18, scale = 6)
    private BigDecimal processGramsOrder;

    @Column(name = "spot_grams_order", nullable = false, precision = 18, scale = 6)
    private BigDecimal spotGramsOrder;

    @Column(name = "total_grams_order", nullable = false, precision = 18, scale = 6)
    private BigDecimal totalGramsOrder;

    @Column(name = "spot_count", nullable = false)
    private int spotCount;

    @Column(name = "icc_profile_used", length = 120)
    private String iccProfileUsed;

    @Column(name = "estimate_date", nullable = false)
    private Instant estimateDate;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "user_id", length = 64)
    private String userId;

    public InkEstimateHistoryEntity() {
    }

    @Override
    public String getId() {
        return inkEstimateHistoryId;
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

    public String getInkEstimateHistoryId() {
        return inkEstimateHistoryId;
    }

    public void setInkEstimateHistoryId(String inkEstimateHistoryId) {
        this.inkEstimateHistoryId = inkEstimateHistoryId;
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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public BigDecimal getWidthCm() {
        return widthCm;
    }

    public void setWidthCm(BigDecimal widthCm) {
        this.widthCm = widthCm;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public int getSheetCount() {
        return sheetCount;
    }

    public void setSheetCount(int sheetCount) {
        this.sheetCount = sheetCount;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public BigDecimal getGramsPerCm2() {
        return gramsPerCm2;
    }

    public void setGramsPerCm2(BigDecimal gramsPerCm2) {
        this.gramsPerCm2 = gramsPerCm2;
    }

    public BigDecimal getProcessGramsOrder() {
        return processGramsOrder;
    }

    public void setProcessGramsOrder(BigDecimal processGramsOrder) {
        this.processGramsOrder = processGramsOrder;
    }

    public BigDecimal getSpotGramsOrder() {
        return spotGramsOrder;
    }

    public void setSpotGramsOrder(BigDecimal spotGramsOrder) {
        this.spotGramsOrder = spotGramsOrder;
    }

    public BigDecimal getTotalGramsOrder() {
        return totalGramsOrder;
    }

    public void setTotalGramsOrder(BigDecimal totalGramsOrder) {
        this.totalGramsOrder = totalGramsOrder;
    }

    public int getSpotCount() {
        return spotCount;
    }

    public void setSpotCount(int spotCount) {
        this.spotCount = spotCount;
    }

    public String getIccProfileUsed() {
        return iccProfileUsed;
    }

    public void setIccProfileUsed(String iccProfileUsed) {
        this.iccProfileUsed = iccProfileUsed;
    }

    public Instant getEstimateDate() {
        return estimateDate;
    }

    public void setEstimateDate(Instant estimateDate) {
        this.estimateDate = estimateDate;
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
}
