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
@Table(name = "paper_stock", schema = "indicolors")
public class PaperStockEntity implements Persistable<String> {

    @Id
    @Column(name = "paper_stock_id", length = 64)
    private String paperStockId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "paper_id", nullable = false, length = 64)
    private String paperId;

    @Column(name = "quantity_initial", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityInitial;

    @Column(name = "quantity_available", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityAvailable;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "state", nullable = false)
    private boolean state;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PaperStockEntity() {
    }

    @Override
    public String getId() {
        return paperStockId;
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

    public String getPaperStockId() {
        return paperStockId;
    }

    public void setPaperStockId(String paperStockId) {
        this.paperStockId = paperStockId;
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

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
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
