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
@Table(name = "production_order_machine_usage", schema = "indicolors")
public class ProductionOrderMachineUsageEntity implements Persistable<String> {

    @Id
    @Column(name = "production_order_machine_usage_id", length = 64)
    private String productionOrderMachineUsageId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(nullable = false, length = 20)
    private String phase;

    @Column(name = "machine_id", nullable = false, length = 64)
    private String machineId;

    @Column(name = "machine_name_snapshot", nullable = false, length = 150)
    private String machineNameSnapshot;

    @Column(name = "cost_per_hour_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPerHourSnapshot;

    @Column(name = "estimated_setup_minutes", nullable = false)
    private int estimatedSetupMinutes;

    @Column(name = "estimated_run_minutes", nullable = false)
    private int estimatedRunMinutes;

    @Column(name = "estimated_machine_cost", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal estimatedMachineCost;

    @Column(name = "actual_setup_minutes")
    private Integer actualSetupMinutes;

    @Column(name = "actual_run_minutes")
    private Integer actualRunMinutes;

    @Column(name = "actual_machine_cost", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal actualMachineCost;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProductionOrderMachineUsageEntity() {
    }

    @Override
    public String getId() {
        return productionOrderMachineUsageId;
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

    public String getProductionOrderMachineUsageId() {
        return productionOrderMachineUsageId;
    }

    public void setProductionOrderMachineUsageId(String productionOrderMachineUsageId) {
        this.productionOrderMachineUsageId = productionOrderMachineUsageId;
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

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getMachineNameSnapshot() {
        return machineNameSnapshot;
    }

    public void setMachineNameSnapshot(String machineNameSnapshot) {
        this.machineNameSnapshot = machineNameSnapshot;
    }

    public BigDecimal getCostPerHourSnapshot() {
        return costPerHourSnapshot;
    }

    public void setCostPerHourSnapshot(BigDecimal costPerHourSnapshot) {
        this.costPerHourSnapshot = costPerHourSnapshot;
    }

    public int getEstimatedSetupMinutes() {
        return estimatedSetupMinutes;
    }

    public void setEstimatedSetupMinutes(int estimatedSetupMinutes) {
        this.estimatedSetupMinutes = estimatedSetupMinutes;
    }

    public int getEstimatedRunMinutes() {
        return estimatedRunMinutes;
    }

    public void setEstimatedRunMinutes(int estimatedRunMinutes) {
        this.estimatedRunMinutes = estimatedRunMinutes;
    }

    public BigDecimal getEstimatedMachineCost() {
        return estimatedMachineCost;
    }

    public void setEstimatedMachineCost(BigDecimal estimatedMachineCost) {
        this.estimatedMachineCost = estimatedMachineCost;
    }

    public Integer getActualSetupMinutes() {
        return actualSetupMinutes;
    }

    public void setActualSetupMinutes(Integer actualSetupMinutes) {
        this.actualSetupMinutes = actualSetupMinutes;
    }

    public Integer getActualRunMinutes() {
        return actualRunMinutes;
    }

    public void setActualRunMinutes(Integer actualRunMinutes) {
        this.actualRunMinutes = actualRunMinutes;
    }

    public BigDecimal getActualMachineCost() {
        return actualMachineCost;
    }

    public void setActualMachineCost(BigDecimal actualMachineCost) {
        this.actualMachineCost = actualMachineCost;
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
