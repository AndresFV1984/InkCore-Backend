package com.inkcore.infrastructure.out.persistence.wastesettings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_waste_settings", schema = "indicolors")
public class CompanyWasteSettingsEntity implements Persistable<String> {

    @Id
    @Column(name = "company_id", length = 64)
    private String companyId;

    @Transient
    private boolean isNew = true;

    @Column(name = "cut_waste_min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal cutWasteMinPercentage;

    @Column(name = "cut_waste_max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal cutWasteMaxPercentage;

    @Column(name = "cut_waste_default_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal cutWasteDefaultPercentage;

    @Column(name = "operational_waste_min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal operationalWasteMinPercentage;

    @Column(name = "operational_waste_max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal operationalWasteMaxPercentage;

    @Column(name = "operational_waste_default_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal operationalWasteDefaultPercentage;

    @Column(name = "prepress_waste_min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal prepressWasteMinPercentage;

    @Column(name = "prepress_waste_max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal prepressWasteMaxPercentage;

    @Column(name = "prepress_waste_default_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal prepressWasteDefaultPercentage;

    @Column(name = "finished_waste_min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal finishedWasteMinPercentage;

    @Column(name = "finished_waste_max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal finishedWasteMaxPercentage;

    @Column(name = "finished_waste_default_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal finishedWasteDefaultPercentage;

    @Column(name = "finishing_waste_min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal finishingWasteMinPercentage;

    @Column(name = "finishing_waste_max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal finishingWasteMaxPercentage;

    @Column(name = "finishing_waste_default_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal finishingWasteDefaultPercentage;

    @Column(name = "cut_makeready_sheets", nullable = false, precision = 12, scale = 2)
    private BigDecimal cutMakereadySheets;

    @Column(name = "operational_makeready_sheets", nullable = false, precision = 12, scale = 2)
    private BigDecimal operationalMakereadySheets;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CompanyWasteSettingsEntity() {
    }

    @Override
    public String getId() {
        return companyId;
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

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public BigDecimal getCutWasteMinPercentage() {
        return cutWasteMinPercentage;
    }

    public void setCutWasteMinPercentage(BigDecimal cutWasteMinPercentage) {
        this.cutWasteMinPercentage = cutWasteMinPercentage;
    }

    public BigDecimal getCutWasteMaxPercentage() {
        return cutWasteMaxPercentage;
    }

    public void setCutWasteMaxPercentage(BigDecimal cutWasteMaxPercentage) {
        this.cutWasteMaxPercentage = cutWasteMaxPercentage;
    }

    public BigDecimal getCutWasteDefaultPercentage() {
        return cutWasteDefaultPercentage;
    }

    public void setCutWasteDefaultPercentage(BigDecimal cutWasteDefaultPercentage) {
        this.cutWasteDefaultPercentage = cutWasteDefaultPercentage;
    }

    public BigDecimal getOperationalWasteMinPercentage() {
        return operationalWasteMinPercentage;
    }

    public void setOperationalWasteMinPercentage(BigDecimal operationalWasteMinPercentage) {
        this.operationalWasteMinPercentage = operationalWasteMinPercentage;
    }

    public BigDecimal getOperationalWasteMaxPercentage() {
        return operationalWasteMaxPercentage;
    }

    public void setOperationalWasteMaxPercentage(BigDecimal operationalWasteMaxPercentage) {
        this.operationalWasteMaxPercentage = operationalWasteMaxPercentage;
    }

    public BigDecimal getOperationalWasteDefaultPercentage() {
        return operationalWasteDefaultPercentage;
    }

    public void setOperationalWasteDefaultPercentage(BigDecimal operationalWasteDefaultPercentage) {
        this.operationalWasteDefaultPercentage = operationalWasteDefaultPercentage;
    }

    public BigDecimal getPrepressWasteMinPercentage() {
        return prepressWasteMinPercentage;
    }

    public void setPrepressWasteMinPercentage(BigDecimal prepressWasteMinPercentage) {
        this.prepressWasteMinPercentage = prepressWasteMinPercentage;
    }

    public BigDecimal getPrepressWasteMaxPercentage() {
        return prepressWasteMaxPercentage;
    }

    public void setPrepressWasteMaxPercentage(BigDecimal prepressWasteMaxPercentage) {
        this.prepressWasteMaxPercentage = prepressWasteMaxPercentage;
    }

    public BigDecimal getPrepressWasteDefaultPercentage() {
        return prepressWasteDefaultPercentage;
    }

    public void setPrepressWasteDefaultPercentage(BigDecimal prepressWasteDefaultPercentage) {
        this.prepressWasteDefaultPercentage = prepressWasteDefaultPercentage;
    }

    public BigDecimal getFinishedWasteMinPercentage() {
        return finishedWasteMinPercentage;
    }

    public void setFinishedWasteMinPercentage(BigDecimal finishedWasteMinPercentage) {
        this.finishedWasteMinPercentage = finishedWasteMinPercentage;
    }

    public BigDecimal getFinishedWasteMaxPercentage() {
        return finishedWasteMaxPercentage;
    }

    public void setFinishedWasteMaxPercentage(BigDecimal finishedWasteMaxPercentage) {
        this.finishedWasteMaxPercentage = finishedWasteMaxPercentage;
    }

    public BigDecimal getFinishedWasteDefaultPercentage() {
        return finishedWasteDefaultPercentage;
    }

    public void setFinishedWasteDefaultPercentage(BigDecimal finishedWasteDefaultPercentage) {
        this.finishedWasteDefaultPercentage = finishedWasteDefaultPercentage;
    }

    public BigDecimal getFinishingWasteMinPercentage() {
        return finishingWasteMinPercentage;
    }

    public void setFinishingWasteMinPercentage(BigDecimal finishingWasteMinPercentage) {
        this.finishingWasteMinPercentage = finishingWasteMinPercentage;
    }

    public BigDecimal getFinishingWasteMaxPercentage() {
        return finishingWasteMaxPercentage;
    }

    public void setFinishingWasteMaxPercentage(BigDecimal finishingWasteMaxPercentage) {
        this.finishingWasteMaxPercentage = finishingWasteMaxPercentage;
    }

    public BigDecimal getFinishingWasteDefaultPercentage() {
        return finishingWasteDefaultPercentage;
    }

    public void setFinishingWasteDefaultPercentage(BigDecimal finishingWasteDefaultPercentage) {
        this.finishingWasteDefaultPercentage = finishingWasteDefaultPercentage;
    }

    public BigDecimal getCutMakereadySheets() {
        return cutMakereadySheets;
    }

    public void setCutMakereadySheets(BigDecimal cutMakereadySheets) {
        this.cutMakereadySheets = cutMakereadySheets;
    }

    public BigDecimal getOperationalMakereadySheets() {
        return operationalMakereadySheets;
    }

    public void setOperationalMakereadySheets(BigDecimal operationalMakereadySheets) {
        this.operationalMakereadySheets = operationalMakereadySheets;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
