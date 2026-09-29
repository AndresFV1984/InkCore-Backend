package com.inkcore.infrastructure.out.persistence.productionorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_order_cost_summary", schema = "indicolors")
public class ProductionOrderCostSummaryEntity {

    @Id
    @Column(name = "production_order_id", length = 64)
    private String productionOrderId;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "estimated_material_cost", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal estimatedMaterialCost;

    @Column(name = "estimated_machine_cost", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal estimatedMachineCost;

    @Column(name = "estimated_waste_cost", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal estimatedWasteCost;

    @Column(name = "estimated_merma_cost", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal estimatedMermaCost;

    @Column(name = "estimated_total_cost", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal estimatedTotalCost;

    @Column(name = "actual_material_cost", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal actualMaterialCost;

    @Column(name = "actual_machine_cost", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal actualMachineCost;

    @Column(name = "actual_waste_cost", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal actualWasteCost;

    @Column(name = "actual_merma_cost", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal actualMermaCost;

    @Column(name = "actual_desperdicio_cost", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal actualDesperdicioCost;

    @Column(name = "actual_total_cost", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal actualTotalCost;

    @Column(name = "quoted_price", precision = 14, scale = 2)
    private BigDecimal quotedPrice;

    @Column(name = "estimated_margin", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal estimatedMargin;

    @Column(name = "actual_margin", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal actualMargin;

    @Column(name = "actual_margin_pct", precision = 6, scale = 2, insertable = false, updatable = false)
    private BigDecimal actualMarginPct;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public String getProductionOrderId() {
        return productionOrderId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public BigDecimal getEstimatedMaterialCost() {
        return estimatedMaterialCost;
    }

    public BigDecimal getEstimatedMachineCost() {
        return estimatedMachineCost;
    }

    public BigDecimal getEstimatedWasteCost() {
        return estimatedWasteCost;
    }

    public BigDecimal getEstimatedMermaCost() {
        return estimatedMermaCost;
    }

    public BigDecimal getEstimatedTotalCost() {
        return estimatedTotalCost;
    }

    public BigDecimal getActualMaterialCost() {
        return actualMaterialCost;
    }

    public BigDecimal getActualMachineCost() {
        return actualMachineCost;
    }

    public BigDecimal getActualWasteCost() {
        return actualWasteCost;
    }

    public BigDecimal getActualMermaCost() {
        return actualMermaCost;
    }

    public BigDecimal getActualDesperdicioCost() {
        return actualDesperdicioCost;
    }

    public BigDecimal getActualTotalCost() {
        return actualTotalCost;
    }

    public BigDecimal getQuotedPrice() {
        return quotedPrice;
    }

    public BigDecimal getEstimatedMargin() {
        return estimatedMargin;
    }

    public BigDecimal getActualMargin() {
        return actualMargin;
    }

    public BigDecimal getActualMarginPct() {
        return actualMarginPct;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
