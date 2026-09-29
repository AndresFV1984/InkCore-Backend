package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uso de máquina por fase. El costo estimado y el real los calcula la BD
 * a partir del snapshot de costo/hora y los minutos.
 */
public final class MachineUsage {

    private String machineUsageId;
    private String companyId;
    private String productionOrderId;
    private String phase;
    private String machineId;
    private String machineNameSnapshot;
    private BigDecimal costPerHourSnapshot;
    private int estimatedSetupMinutes;
    private int estimatedRunMinutes;
    private BigDecimal estimatedMachineCost;
    private Integer actualSetupMinutes;
    private Integer actualRunMinutes;
    private BigDecimal actualMachineCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MachineUsage() {
        this.machineUsageId = UUID.randomUUID().toString();
    }

    public String getMachineUsageId() {
        return machineUsageId;
    }

    public void setMachineUsageId(String machineUsageId) {
        this.machineUsageId = machineUsageId;
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
