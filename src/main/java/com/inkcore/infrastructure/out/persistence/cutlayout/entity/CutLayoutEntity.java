package com.inkcore.infrastructure.out.persistence.cutlayout.entity;

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
@Table(name = "cut_layouts", schema = "indicolors")
public class CutLayoutEntity implements Persistable<String> {

    @Id
    @Column(name = "cut_layout_id", length = 64)
    private String cutLayoutId;

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

    @Column(name = "pieces_per_sheet", nullable = false)
    private int piecesPerSheet;

    @Column(nullable = false)
    private boolean state;

    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    public CutLayoutEntity() {
    }

    @Override
    public String getId() {
        return cutLayoutId;
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

    public String getCutLayoutId() {
        return cutLayoutId;
    }

    public void setCutLayoutId(String cutLayoutId) {
        this.cutLayoutId = cutLayoutId;
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

    public int getPiecesPerSheet() {
        return piecesPerSheet;
    }

    public void setPiecesPerSheet(int piecesPerSheet) {
        this.piecesPerSheet = piecesPerSheet;
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
