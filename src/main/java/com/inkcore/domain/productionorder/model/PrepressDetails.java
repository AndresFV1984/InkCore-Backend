package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Detalle de Preprensa, 1:1 con la orden
 * ({@code indicolors.production_order_prepress_details}).
 */
public final class PrepressDetails {

    private String productionOrderId;
    private String companyId;

    private Boolean newDesign;
    private String designName;
    private String existingDesignOrderId;
    private boolean hasDesignCost;
    private BigDecimal designCost;
    private Boolean clientSuppliesPlates;
    private ClientPlateType clientPlateType;
    private BigDecimal newPlateCost;
    private String assemblyPriceId;
    private String assemblyPriceName;
    private BigDecimal assemblyPriceCost;
    private boolean dieCutLine;
    private boolean uvReserve;
    private boolean stamping;
    private boolean embossing;
    private BigDecimal totalPlatesValue;
    private DiscountType prepressDiscountType;
    private BigDecimal prepressDiscountValue;
    private LocalDateTime prepressCompletedAt;

    public PrepressDetails() {
    }

    public String getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(String productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public Boolean getNewDesign() {
        return newDesign;
    }

    public void setNewDesign(Boolean newDesign) {
        this.newDesign = newDesign;
    }

    public String getDesignName() {
        return designName;
    }

    public void setDesignName(String designName) {
        this.designName = designName;
    }

    public String getExistingDesignOrderId() {
        return existingDesignOrderId;
    }

    public void setExistingDesignOrderId(String existingDesignOrderId) {
        this.existingDesignOrderId = existingDesignOrderId;
    }

    public boolean isHasDesignCost() {
        return hasDesignCost;
    }

    public void setHasDesignCost(boolean hasDesignCost) {
        this.hasDesignCost = hasDesignCost;
    }

    public BigDecimal getDesignCost() {
        return designCost;
    }

    public void setDesignCost(BigDecimal designCost) {
        this.designCost = designCost;
    }

    public Boolean getClientSuppliesPlates() {
        return clientSuppliesPlates;
    }

    public void setClientSuppliesPlates(Boolean clientSuppliesPlates) {
        this.clientSuppliesPlates = clientSuppliesPlates;
    }

    public ClientPlateType getClientPlateType() {
        return clientPlateType;
    }

    public void setClientPlateType(ClientPlateType clientPlateType) {
        this.clientPlateType = clientPlateType;
    }

    public BigDecimal getNewPlateCost() {
        return newPlateCost;
    }

    public void setNewPlateCost(BigDecimal newPlateCost) {
        this.newPlateCost = newPlateCost;
    }

    public String getAssemblyPriceId() {
        return assemblyPriceId;
    }

    public void setAssemblyPriceId(String assemblyPriceId) {
        this.assemblyPriceId = assemblyPriceId;
    }

    public String getAssemblyPriceName() {
        return assemblyPriceName;
    }

    public void setAssemblyPriceName(String assemblyPriceName) {
        this.assemblyPriceName = assemblyPriceName;
    }

    public BigDecimal getAssemblyPriceCost() {
        return assemblyPriceCost;
    }

    public void setAssemblyPriceCost(BigDecimal assemblyPriceCost) {
        this.assemblyPriceCost = assemblyPriceCost;
    }

    public boolean isDieCutLine() {
        return dieCutLine;
    }

    public void setDieCutLine(boolean dieCutLine) {
        this.dieCutLine = dieCutLine;
    }

    public boolean isUvReserve() {
        return uvReserve;
    }

    public void setUvReserve(boolean uvReserve) {
        this.uvReserve = uvReserve;
    }

    public boolean isStamping() {
        return stamping;
    }

    public void setStamping(boolean stamping) {
        this.stamping = stamping;
    }

    public boolean isEmbossing() {
        return embossing;
    }

    public void setEmbossing(boolean embossing) {
        this.embossing = embossing;
    }

    public BigDecimal getTotalPlatesValue() {
        return totalPlatesValue;
    }

    public void setTotalPlatesValue(BigDecimal totalPlatesValue) {
        this.totalPlatesValue = totalPlatesValue;
    }

    public DiscountType getPrepressDiscountType() {
        return prepressDiscountType;
    }

    public void setPrepressDiscountType(DiscountType prepressDiscountType) {
        this.prepressDiscountType = prepressDiscountType;
    }

    public BigDecimal getPrepressDiscountValue() {
        return prepressDiscountValue;
    }

    public void setPrepressDiscountValue(BigDecimal prepressDiscountValue) {
        this.prepressDiscountValue = prepressDiscountValue;
    }

    public LocalDateTime getPrepressCompletedAt() {
        return prepressCompletedAt;
    }

    public void setPrepressCompletedAt(LocalDateTime prepressCompletedAt) {
        this.prepressCompletedAt = prepressCompletedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PrepressDetails that)) {
            return false;
        }
        return Objects.equals(productionOrderId, that.productionOrderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productionOrderId);
    }
}
