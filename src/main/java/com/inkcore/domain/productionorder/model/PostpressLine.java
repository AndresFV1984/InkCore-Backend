package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Línea de costeo de Terminados/Acabados
 * ({@code indicolors.production_order_postpress_lines}).
 */
public final class PostpressLine {

    private String lineId;
    private String companyId;
    private String recordId;

    private String catalogItemId;
    private String itemName;
    private String source;

    private BigDecimal valuePerCm2;
    private BigDecimal minCost;
    private BigDecimal areaFactor;
    private Integer goodSizes;
    private BigDecimal calculatedPrice;
    private BigDecimal chargedPrice;
    private boolean appliedMinCost;

    private Boolean positive;
    private Boolean cliche;

    public PostpressLine() {
        this.lineId = UUID.randomUUID().toString();
    }

    public String getLineId() {
        return lineId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PostpressLine that)) {
            return false;
        }
        return Objects.equals(lineId, that.lineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineId);
    }
}
