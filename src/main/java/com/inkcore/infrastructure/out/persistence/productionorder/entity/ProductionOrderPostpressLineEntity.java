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
@Table(name = "production_order_postpress_lines", schema = "indicolors")
public class ProductionOrderPostpressLineEntity implements Persistable<String> {

    @Id
    @Column(name = "production_order_postpress_line_id", length = 64)
    private String productionOrderPostpressLineId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "record_id", nullable = false, length = 64)
    private String recordId;

    @Column(name = "catalog_item_id", length = 64)
    private String catalogItemId;

    @Column(name = "item_name", length = 150)
    private String itemName;

    @Column(name = "source", nullable = false, length = 15)
    private String source;

    @Column(name = "value_per_cm2", precision = 12, scale = 4)
    private BigDecimal valuePerCm2;

    @Column(name = "min_cost", precision = 12, scale = 2)
    private BigDecimal minCost;

    @Column(name = "area_factor", precision = 10, scale = 4)
    private BigDecimal areaFactor;

    @Column(name = "good_sizes")
    private Integer goodSizes;

    @Column(name = "calculated_price", precision = 12, scale = 2)
    private BigDecimal calculatedPrice;

    @Column(name = "charged_price", precision = 12, scale = 2)
    private BigDecimal chargedPrice;

    @Column(name = "applied_min_cost", nullable = false)
    private boolean appliedMinCost;

    @Column(name = "positive")
    private Boolean positive;

    @Column(name = "cliche")
    private Boolean cliche;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ProductionOrderPostpressLineEntity() {
    }

    @Override
    public String getId() {
        return productionOrderPostpressLineId;
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

    public String getProductionOrderPostpressLineId() {
        return productionOrderPostpressLineId;
    }

    public void setProductionOrderPostpressLineId(String productionOrderPostpressLineId) {
        this.productionOrderPostpressLineId = productionOrderPostpressLineId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(String catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public BigDecimal getValuePerCm2() {
        return valuePerCm2;
    }

    public void setValuePerCm2(BigDecimal valuePerCm2) {
        this.valuePerCm2 = valuePerCm2;
    }

    public BigDecimal getMinCost() {
        return minCost;
    }

    public void setMinCost(BigDecimal minCost) {
        this.minCost = minCost;
    }

    public BigDecimal getAreaFactor() {
        return areaFactor;
    }

    public void setAreaFactor(BigDecimal areaFactor) {
        this.areaFactor = areaFactor;
    }

    public Integer getGoodSizes() {
        return goodSizes;
    }

    public void setGoodSizes(Integer goodSizes) {
        this.goodSizes = goodSizes;
    }

    public BigDecimal getCalculatedPrice() {
        return calculatedPrice;
    }

    public void setCalculatedPrice(BigDecimal calculatedPrice) {
        this.calculatedPrice = calculatedPrice;
    }

    public BigDecimal getChargedPrice() {
        return chargedPrice;
    }

    public void setChargedPrice(BigDecimal chargedPrice) {
        this.chargedPrice = chargedPrice;
    }

    public boolean isAppliedMinCost() {
        return appliedMinCost;
    }

    public void setAppliedMinCost(boolean appliedMinCost) {
        this.appliedMinCost = appliedMinCost;
    }

    public Boolean getPositive() {
        return positive;
    }

    public void setPositive(Boolean positive) {
        this.positive = positive;
    }

    public Boolean getCliche() {
        return cliche;
    }

    public void setCliche(Boolean cliche) {
        this.cliche = cliche;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
