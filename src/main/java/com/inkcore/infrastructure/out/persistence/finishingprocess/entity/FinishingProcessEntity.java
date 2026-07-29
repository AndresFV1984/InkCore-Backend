package com.inkcore.infrastructure.out.persistence.finishingprocess.entity;

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

@Entity
@Table(name = "finishing_processes", schema = "indicolors")
public class FinishingProcessEntity implements Persistable<String> {

    @Id
    @Column(name = "finishing_process_id", length = 64)
    private String finishingProcessId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "min_cost", precision = 12, scale = 2)
    private BigDecimal minCost;

    @Column(name = "value_per_cm2", nullable = false, precision = 6, scale = 2)
    private BigDecimal valuePerCm2;

    @Column(name = "quick_access", nullable = false)
    private boolean quickAccess;

    @Column(nullable = false)
    private boolean state;

    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    public FinishingProcessEntity() {
    }

    @Override
    public String getId() {
        return finishingProcessId;
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

    public String getFinishingProcessId() {
        return finishingProcessId;
    }

    public void setFinishingProcessId(String finishingProcessId) {
        this.finishingProcessId = finishingProcessId;
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

    public BigDecimal getMinCost() {
        return minCost;
    }

    public void setMinCost(BigDecimal minCost) {
        this.minCost = minCost;
    }

    public BigDecimal getValuePerCm2() {
        return valuePerCm2;
    }

    public void setValuePerCm2(BigDecimal valuePerCm2) {
        this.valuePerCm2 = valuePerCm2;
    }

    public boolean isQuickAccess() {
        return quickAccess;
    }

    public void setQuickAccess(boolean quickAccess) {
        this.quickAccess = quickAccess;
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
}
