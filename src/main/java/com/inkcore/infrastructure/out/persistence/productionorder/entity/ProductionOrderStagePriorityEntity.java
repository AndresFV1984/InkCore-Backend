package com.inkcore.infrastructure.out.persistence.productionorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "production_order_stage_priorities", schema = "indicolors")
public class ProductionOrderStagePriorityEntity {

    @Id
    @Column(name = "stage_priority_id", length = 64)
    private String stagePriorityId;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(name = "stage_code", nullable = false, length = 32)
    private String stageCode;

    @Column(name = "priority_rank", nullable = false)
    private int priorityRank;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    public String getStagePriorityId() {
        return stagePriorityId;
    }

    public void setStagePriorityId(String stagePriorityId) {
        this.stagePriorityId = stagePriorityId;
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

    public String getStageCode() {
        return stageCode;
    }

    public void setStageCode(String stageCode) {
        this.stageCode = stageCode;
    }

    public int getPriorityRank() {
        return priorityRank;
    }

    public void setPriorityRank(int priorityRank) {
        this.priorityRank = priorityRank;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
