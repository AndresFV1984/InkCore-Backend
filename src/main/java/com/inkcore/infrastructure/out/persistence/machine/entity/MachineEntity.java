package com.inkcore.infrastructure.out.persistence.machine.entity;

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
@Table(name = "machines", schema = "indicolors")
public class MachineEntity implements Persistable<String> {

    @Id
    @Column(name = "machine_id", length = 64)
    private String machineId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "machine_type", nullable = false, length = 20)
    private String machineType;

    @Column(length = 150)
    private String manufacturer;

    @Column(length = 150)
    private String model;

    @Column(name = "purchase_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal purchaseCost;

    @Column(name = "useful_life_years", nullable = false, precision = 5, scale = 2)
    private BigDecimal usefulLifeYears;

    @Column(name = "annual_maintenance_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal annualMaintenanceCost;

    @Column(name = "monthly_operator_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyOperatorCost;

    @Column(name = "energy_cost_per_hour", nullable = false, precision = 12, scale = 2)
    private BigDecimal energyCostPerHour;

    @Column(name = "productive_hours_per_year", nullable = false, precision = 10, scale = 2)
    private BigDecimal productiveHoursPerYear;

    @Column(name = "cost_per_hour", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal costPerHour;

    @Column(nullable = false)
    private boolean state;

    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MachineEntity() {
    }

    @Override
    public String getId() {
        return machineId;
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

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(BigDecimal purchaseCost) {
        this.purchaseCost = purchaseCost;
    }

    public BigDecimal getUsefulLifeYears() {
        return usefulLifeYears;
    }

    public void setUsefulLifeYears(BigDecimal usefulLifeYears) {
        this.usefulLifeYears = usefulLifeYears;
    }

    public BigDecimal getAnnualMaintenanceCost() {
        return annualMaintenanceCost;
    }

    public void setAnnualMaintenanceCost(BigDecimal annualMaintenanceCost) {
        this.annualMaintenanceCost = annualMaintenanceCost;
    }

    public BigDecimal getMonthlyOperatorCost() {
        return monthlyOperatorCost;
    }

    public void setMonthlyOperatorCost(BigDecimal monthlyOperatorCost) {
        this.monthlyOperatorCost = monthlyOperatorCost;
    }

    public BigDecimal getEnergyCostPerHour() {
        return energyCostPerHour;
    }

    public void setEnergyCostPerHour(BigDecimal energyCostPerHour) {
        this.energyCostPerHour = energyCostPerHour;
    }

    public BigDecimal getProductiveHoursPerYear() {
        return productiveHoursPerYear;
    }

    public void setProductiveHoursPerYear(BigDecimal productiveHoursPerYear) {
        this.productiveHoursPerYear = productiveHoursPerYear;
    }

    public BigDecimal getCostPerHour() {
        return costPerHour;
    }

    public void setCostPerHour(BigDecimal costPerHour) {
        this.costPerHour = costPerHour;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
