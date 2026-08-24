package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Plancha configurada en Preprensa ({@code indicolors.production_order_plates}).
 */
public final class Plate {

    private String plateId;
    private String companyId;
    private String productionOrderId;

    private String colors;
    private String plateTypeId;
    private String plateName;
    private String plateSize;
    private BigDecimal platePrice;

    private int quantity;
    private Integer cavities;
    private Integer goodSizes;
    private Integer surplus;
    private Integer platesCount;
    private BigDecimal totalValue;

    private String detail;
    private String observation;
    private boolean manualEntry;
    private String plateSupply;

    private boolean plateReplacement;
    private Integer replacementQuantity;

    public Plate() {
        this.plateId = UUID.randomUUID().toString();
    }

    public String getPlateId() {
        return plateId;
    }

    public void setPlateId(String plateId) {
        this.plateId = plateId;
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

    public String getColors() {
        return colors;
    }

    public void setColors(String colors) {
        this.colors = colors;
    }

    public String getPlateTypeId() {
        return plateTypeId;
    }

    public void setPlateTypeId(String plateTypeId) {
        this.plateTypeId = plateTypeId;
    }

    public String getPlateName() {
        return plateName;
    }

    public void setPlateName(String plateName) {
        this.plateName = plateName;
    }

    public String getPlateSize() {
        return plateSize;
    }

    public void setPlateSize(String plateSize) {
        this.plateSize = plateSize;
    }

    public BigDecimal getPlatePrice() {
        return platePrice;
    }

    public void setPlatePrice(BigDecimal platePrice) {
        this.platePrice = platePrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Integer getCavities() {
        return cavities;
    }

    public void setCavities(Integer cavities) {
        this.cavities = cavities;
    }

    public Integer getGoodSizes() {
        return goodSizes;
    }

    public void setGoodSizes(Integer goodSizes) {
        this.goodSizes = goodSizes;
    }

    public Integer getSurplus() {
        return surplus;
    }

    public void setSurplus(Integer surplus) {
        this.surplus = surplus;
    }

    public Integer getPlatesCount() {
        return platesCount;
    }

    public void setPlatesCount(Integer platesCount) {
        this.platesCount = platesCount;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public boolean isManualEntry() {
        return manualEntry;
    }

    public void setManualEntry(boolean manualEntry) {
        this.manualEntry = manualEntry;
    }

    public String getPlateSupply() {
        return plateSupply;
    }

    public void setPlateSupply(String plateSupply) {
        this.plateSupply = plateSupply;
    }

    public boolean isPlateReplacement() {
        return plateReplacement;
    }

    public void setPlateReplacement(boolean plateReplacement) {
        this.plateReplacement = plateReplacement;
    }

    public Integer getReplacementQuantity() {
        return replacementQuantity;
    }

    public void setReplacementQuantity(Integer replacementQuantity) {
        this.replacementQuantity = replacementQuantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Plate that)) {
            return false;
        }
        return Objects.equals(plateId, that.plateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plateId);
    }
}
