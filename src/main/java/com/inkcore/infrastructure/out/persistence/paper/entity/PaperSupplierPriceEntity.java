package com.inkcore.infrastructure.out.persistence.paper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "paper_supplier_prices", schema = "indicolors")
public class PaperSupplierPriceEntity implements Persistable<String> {

    @Id
    @Column(name = "paper_supplier_price_id", length = 64)
    private String paperSupplierPriceId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "paper_id", nullable = false, length = 64)
    private String paperId;

    @Column(name = "supplier_id", nullable = false, length = 64)
    private String supplierId;

    @Column(name = "sheet_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal sheetValue;

    @Column(name = "package_unit", nullable = false)
    private int packageUnit;

    @Column(name = "freight_per_sheet", nullable = false, precision = 12, scale = 2)
    private BigDecimal freightPerSheet;

    @Column(name = "min_purchase_sheets")
    private Integer minPurchaseSheets;

    @Column(name = "payment_days")
    private Integer paymentDays;

    @Column(name = "delivery_days")
    private Integer deliveryDays;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(nullable = false)
    private boolean preferred;

    @Column(nullable = false)
    private boolean state;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "landed_cost_per_sheet", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal landedCostPerSheet;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PaperSupplierPriceEntity() {
    }

    @Override
    public String getId() {
        return paperSupplierPriceId;
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

    public String getPaperSupplierPriceId() {
        return paperSupplierPriceId;
    }

    public void setPaperSupplierPriceId(String paperSupplierPriceId) {
        this.paperSupplierPriceId = paperSupplierPriceId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(String paperId) {
        this.paperId = paperId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
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

    public BigDecimal getFreightPerSheet() {
        return freightPerSheet;
    }

    public void setFreightPerSheet(BigDecimal freightPerSheet) {
        this.freightPerSheet = freightPerSheet;
    }

    public Integer getMinPurchaseSheets() {
        return minPurchaseSheets;
    }

    public void setMinPurchaseSheets(Integer minPurchaseSheets) {
        this.minPurchaseSheets = minPurchaseSheets;
    }

    public Integer getPaymentDays() {
        return paymentDays;
    }

    public void setPaymentDays(Integer paymentDays) {
        this.paymentDays = paymentDays;
    }

    public Integer getDeliveryDays() {
        return deliveryDays;
    }

    public void setDeliveryDays(Integer deliveryDays) {
        this.deliveryDays = deliveryDays;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public void setPreferred(boolean preferred) {
        this.preferred = preferred;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public BigDecimal getLandedCostPerSheet() {
        return landedCostPerSheet;
    }

    public void setLandedCostPerSheet(BigDecimal landedCostPerSheet) {
        this.landedCostPerSheet = landedCostPerSheet;
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
