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

/**
 * 1:1 con {@code production_orders}: comparte la misma clave primaria.
 */
@Entity
@Table(name = "production_order_prepress_details", schema = "indicolors")
public class ProductionOrderPrepressDetailsEntity implements Persistable<String> {

    @Id
    @Column(name = "production_order_id", length = 64)
    private String productionOrderId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "is_new_design")
    private Boolean newDesign;

    @Column(name = "design_name", length = 150)
    private String designName;

    @Column(name = "existing_design_order_id", length = 64)
    private String existingDesignOrderId;

    @Column(name = "has_design_cost", nullable = false)
    private boolean hasDesignCost;

    @Column(name = "design_cost", precision = 12, scale = 2)
    private BigDecimal designCost;

    @Column(name = "client_supplies_plates")
    private Boolean clientSuppliesPlates;

    @Column(name = "client_plate_type", length = 20)
    private String clientPlateType;

    @Column(name = "new_plate_cost", precision = 12, scale = 2)
    private BigDecimal newPlateCost;

    @Column(name = "assembly_price_id", length = 64)
    private String assemblyPriceId;

    @Column(name = "assembly_price_name", length = 150)
    private String assemblyPriceName;

    @Column(name = "assembly_price_cost", precision = 12, scale = 2)
    private BigDecimal assemblyPriceCost;

    @Column(name = "die_cut_line", nullable = false)
    private boolean dieCutLine;

    @Column(name = "uv_reserve", nullable = false)
    private boolean uvReserve;

    @Column(name = "stamping", nullable = false)
    private boolean stamping;

    @Column(name = "embossing", nullable = false)
    private boolean embossing;

    @Column(name = "total_plates_value", precision = 12, scale = 2)
    private BigDecimal totalPlatesValue;

    @Column(name = "prepress_discount_type", length = 10)
    private String prepressDiscountType;

    @Column(name = "prepress_discount_value", precision = 12, scale = 2)
    private BigDecimal prepressDiscountValue;

    @Column(name = "prepress_completed_at")
    private LocalDateTime prepressCompletedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProductionOrderPrepressDetailsEntity() {
    }

    @Override
    public String getId() {
        return productionOrderId;
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

    public String getClientPlateType() {
        return clientPlateType;
    }

    public void setClientPlateType(String clientPlateType) {
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

    public String getPrepressDiscountType() {
        return prepressDiscountType;
    }

    public void setPrepressDiscountType(String prepressDiscountType) {
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
