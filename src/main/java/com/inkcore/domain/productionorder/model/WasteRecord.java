package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Merma planificada o desperdicio real. {@code planned_cost} y {@code actual_cost}
 * los calcula la columna generada ({@code cantidad * unit_cost_snapshot}).
 */
public final class WasteRecord {

    private String wasteRecordId;
    private String companyId;
    private String productionOrderId;
    private String phase;
    private String wasteCategory;
    private String wasteOrigin;
    private String materialType;
    private String paperRowId;
    private String postpressLineId;
    private BigDecimal plannedQuantity;
    private BigDecimal actualQuantity;
    private BigDecimal unitCostSnapshot;
    private BigDecimal plannedCost;
    private BigDecimal actualCost;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WasteRecord() {
        this.wasteRecordId = UUID.randomUUID().toString();
        this.plannedQuantity = BigDecimal.ZERO;
        this.wasteOrigin = "exceso";
    }

    public String getWasteRecordId() {
        return wasteRecordId;
    }

    public void setWasteRecordId(String wasteRecordId) {
        this.wasteRecordId = wasteRecordId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(String productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getWasteCategory() {
        return wasteCategory;
    }

    public void setWasteCategory(String wasteCategory) {
        this.wasteCategory = wasteCategory;
    }

    public String getWasteOrigin() {
        return wasteOrigin;
    }

    public void setWasteOrigin(String wasteOrigin) {
        this.wasteOrigin = wasteOrigin;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getPaperRowId() {
        return paperRowId;
    }

    public void setPaperRowId(String paperRowId) {
        this.paperRowId = paperRowId;
    }

    public String getPostpressLineId() {
        return postpressLineId;
    }

    public void setPostpressLineId(String postpressLineId) {
        this.postpressLineId = postpressLineId;
    }

    public BigDecimal getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(BigDecimal plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }

    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public BigDecimal getUnitCostSnapshot() {
        return unitCostSnapshot;
    }

    public void setUnitCostSnapshot(BigDecimal unitCostSnapshot) {
        this.unitCostSnapshot = unitCostSnapshot;
    }

    public BigDecimal getPlannedCost() {
        return plannedCost;
    }

    public void setPlannedCost(BigDecimal plannedCost) {
        this.plannedCost = plannedCost;
    }

    public BigDecimal getActualCost() {
        return actualCost;
    }

    public void setActualCost(BigDecimal actualCost) {
        this.actualCost = actualCost;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
