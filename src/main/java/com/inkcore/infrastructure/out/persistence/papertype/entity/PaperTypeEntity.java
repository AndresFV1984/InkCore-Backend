package com.inkcore.infrastructure.out.persistence.papertype.entity;

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
@Table(name = "paper_types", schema = "indicolors")
public class PaperTypeEntity implements Persistable<String> {

    @Id
    @Column(name = "paper_type_id", length = 64)
    private String paperTypeId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal width;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal height;

    @Column(nullable = false, length = 10)
    private String unit;

    @Column(name = "sheet_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal sheetValue;

    @Column(name = "package_unit", nullable = false)
    private int packageUnit;

    @Column(name = "is_coated", nullable = false)
    private boolean coated;

    @Column(nullable = false)
    private boolean state;

    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    public PaperTypeEntity() {
    }

    @Override
    public String getId() {
        return paperTypeId;
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

    public String getPaperTypeId() {
        return paperTypeId;
    }

    public void setPaperTypeId(String paperTypeId) {
        this.paperTypeId = paperTypeId;
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

    public BigDecimal getSheetValue() {
        return sheetValue;
    }

    public void setSheetValue(BigDecimal sheetValue) {
        this.sheetValue = sheetValue;
    }

    public int getPackageUnit() {
        return packageUnit;
    }

    public void setPackageUnit(int packageUnit) {
        this.packageUnit = packageUnit;
    }

    public boolean isCoated() {
        return coated;
    }

    public void setCoated(boolean coated) {
        this.coated = coated;
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
