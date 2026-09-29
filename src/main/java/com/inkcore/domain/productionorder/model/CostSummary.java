package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lectura de {@code production_order_cost_summary}. Las columnas de costo y
 * margen las mantiene el trigger; el backend solo escribe {@code quoted_price}.
 */
public final class CostSummary {

    private final String productionOrderId;
    private final String companyId;
    private final BigDecimal estimatedMaterialCost;
    private final BigDecimal estimatedMachineCost;
    private final BigDecimal estimatedWasteCost;
    private final BigDecimal estimatedMermaCost;
    private final BigDecimal estimatedTotalCost;
    private final BigDecimal actualMaterialCost;
    private final BigDecimal actualMachineCost;
    private final BigDecimal actualWasteCost;
    private final BigDecimal actualMermaCost;
    private final BigDecimal actualDesperdicioCost;
    private final BigDecimal actualTotalCost;
    private final BigDecimal quotedPrice;
    private final BigDecimal estimatedMargin;
    private final BigDecimal actualMargin;
    private final BigDecimal actualMarginPct;
    private final LocalDateTime updatedAt;

    public CostSummary(
            String productionOrderId,
            String companyId,
            BigDecimal estimatedMaterialCost,
            BigDecimal estimatedMachineCost,
            BigDecimal estimatedWasteCost,
            BigDecimal estimatedMermaCost,
            BigDecimal estimatedTotalCost,
            BigDecimal actualMaterialCost,
            BigDecimal actualMachineCost,
            BigDecimal actualWasteCost,
            BigDecimal actualMermaCost,
            BigDecimal actualDesperdicioCost,
            BigDecimal actualTotalCost,
            BigDecimal quotedPrice,
            BigDecimal estimatedMargin,
            BigDecimal actualMargin,
            BigDecimal actualMarginPct,
            LocalDateTime updatedAt
    ) {
        this.productionOrderId = productionOrderId;
        this.companyId = companyId;
        this.estimatedMaterialCost = estimatedMaterialCost;
        this.estimatedMachineCost = estimatedMachineCost;
        this.estimatedWasteCost = estimatedWasteCost;
        this.estimatedMermaCost = estimatedMermaCost;
        this.estimatedTotalCost = estimatedTotalCost;
        this.actualMaterialCost = actualMaterialCost;
        this.actualMachineCost = actualMachineCost;
        this.actualWasteCost = actualWasteCost;
        this.actualMermaCost = actualMermaCost;
        this.actualDesperdicioCost = actualDesperdicioCost;
        this.actualTotalCost = actualTotalCost;
        this.quotedPrice = quotedPrice;
        this.estimatedMargin = estimatedMargin;
        this.actualMargin = actualMargin;
        this.actualMarginPct = actualMarginPct;
        this.updatedAt = updatedAt;
    }

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
