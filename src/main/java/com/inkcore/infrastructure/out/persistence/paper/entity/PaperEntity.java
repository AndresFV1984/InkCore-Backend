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
@Table(name = "papers", schema = "indicolors")
public class PaperEntity implements Persistable<String> {

    @Id
    @Column(name = "paper_id", length = 64)
    private String paperId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(precision = 6, scale = 2)
    private BigDecimal grammage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal width;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal height;

    @Column(nullable = false, length = 10)
    private String unit;

    @Column(name = "is_coated", nullable = false)
    private boolean coated;

    @Column(name = "accepts_remnants", nullable = false)
    private boolean acceptsRemnants;

    @Column(name = "min_remnant_width", precision = 10, scale = 2)
    private BigDecimal minRemnantWidth;

    @Column(name = "min_remnant_height", precision = 10, scale = 2)
    private BigDecimal minRemnantHeight;

    @Column(name = "min_remnant_unit", length = 10)
    private String minRemnantUnit;

    @Column(nullable = false)
    private boolean state;

    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PaperEntity() {
    }

    @Override
    public String getId() {
        return paperId;
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

    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(String paperId) {
        this.paperId = paperId;
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

    public BigDecimal getGrammage() {
        return grammage;
    }

    public void setGrammage(BigDecimal grammage) {
        this.grammage = grammage;
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

    public boolean isCoated() {
        return coated;
    }

    public void setCoated(boolean coated) {
        this.coated = coated;
    }

    public boolean isAcceptsRemnants() {
        return acceptsRemnants;
    }

    public void setAcceptsRemnants(boolean acceptsRemnants) {
        this.acceptsRemnants = acceptsRemnants;
    }

    public BigDecimal getMinRemnantWidth() {
        return minRemnantWidth;
    }

    public void setMinRemnantWidth(BigDecimal minRemnantWidth) {
        this.minRemnantWidth = minRemnantWidth;
    }

    public BigDecimal getMinRemnantHeight() {
        return minRemnantHeight;
    }

    public void setMinRemnantHeight(BigDecimal minRemnantHeight) {
        this.minRemnantHeight = minRemnantHeight;
    }

    public String getMinRemnantUnit() {
        return minRemnantUnit;
    }

    public void setMinRemnantUnit(String minRemnantUnit) {
        this.minRemnantUnit = minRemnantUnit;
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
