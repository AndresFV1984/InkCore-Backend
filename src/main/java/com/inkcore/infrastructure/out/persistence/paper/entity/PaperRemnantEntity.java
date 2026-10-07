package com.inkcore.infrastructure.out.persistence.paper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "paper_remnants", schema = "indicolors")
public class PaperRemnantEntity implements Persistable<String> {

    @Id
    @Column(name = "paper_remnant_id", length = 64)
    private String paperRemnantId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "paper_id", nullable = false, length = 64)
    private String paperId;

    @Column(name = "width", nullable = false, precision = 10, scale = 2)
    private BigDecimal width;

    @Column(name = "height", nullable = false, precision = 10, scale = 2)
    private BigDecimal height;

    @Column(name = "unit", nullable = false, length = 10)
    private String unit;

    @Column(name = "quantity_initial", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityInitial;

    @Column(name = "quantity_available", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityAvailable;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "source_production_order_id", length = 64)
    private String sourceProductionOrderId;

    @Column(name = "source_paper_row_id", length = 64)
    private String sourcePaperRowId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "state", nullable = false)
    private boolean state;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PaperRemnantEntity() {
    }

    @Override
    public String getId() {
        return paperRemnantId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    public String getPaperRemnantId() {
        return paperRemnantId;
    }

    public void setPaperRemnantId(String paperRemnantId) {
        this.paperRemnantId = paperRemnantId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(String paperId) {
        this.paperId = paperId;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public void setWidth(BigDecimal width) {
        this.width = width;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getQuantityInitial() {
        return quantityInitial;
    }

    public void setQuantityInitial(BigDecimal quantityInitial) {
        this.quantityInitial = quantityInitial;
    }

    public BigDecimal getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(BigDecimal quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public String getSourceProductionOrderId() {
        return sourceProductionOrderId;
    }

    public void setSourceProductionOrderId(String sourceProductionOrderId) {
        this.sourceProductionOrderId = sourceProductionOrderId;
    }

    public String getSourcePaperRowId() {
        return sourcePaperRowId;
    }

    public void setSourcePaperRowId(String sourcePaperRowId) {
        this.sourcePaperRowId = sourcePaperRowId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
