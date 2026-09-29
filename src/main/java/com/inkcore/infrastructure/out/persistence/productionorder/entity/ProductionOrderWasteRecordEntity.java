package com.inkcore.infrastructure.out.persistence.productionorder.entity;

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
@Table(name = "production_order_waste_records", schema = "indicolors")
public class ProductionOrderWasteRecordEntity implements Persistable<String> {

    @Id
    @Column(name = "production_order_waste_record_id", length = 64)
    private String productionOrderWasteRecordId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(nullable = false, length = 20)
    private String phase;

    @Column(name = "waste_category", nullable = false, length = 20)
    private String wasteCategory;

    @Column(name = "waste_origin", nullable = false, length = 20)
    private String wasteOrigin;

    @Column(name = "material_type", nullable = false, length = 20)
    private String materialType;

    @Column(name = "paper_row_id", length = 64)
    private String paperRowId;

    @Column(name = "postpress_line_id", length = 64)
    private String postpressLineId;

    @Column(name = "planned_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal plannedQuantity;

    @Column(name = "actual_quantity", precision = 12, scale = 2)
    private BigDecimal actualQuantity;

    @Column(name = "unit_cost_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCostSnapshot;

    @Column(name = "planned_cost", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal plannedCost;

    @Column(name = "actual_cost", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal actualCost;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProductionOrderWasteRecordEntity() {
    }

    @Override
    public String getId() {
        return productionOrderWasteRecordId;
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

    public String getProductionOrderWasteRecordId() {
        return productionOrderWasteRecordId;
    }

    public void setProductionOrderWasteRecordId(String productionOrderWasteRecordId) {
        this.productionOrderWasteRecordId = productionOrderWasteRecordId;
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
