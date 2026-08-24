package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Descuento por etapa ({@code indicolors.production_order_stage_discounts}).
 * Solo aplica a CUTTING, FINISHED_PRODUCTS y FINISHING_PROCESSES: Preprensa y
 * Cobro llevan su descuento en su propia tabla 1:1.
 */
public final class StageDiscount {

    private String stageDiscountId;
    private String companyId;
    private String productionOrderId;
    private ProductionOrderStage stage;
    private DiscountType discountType;
    private BigDecimal discountValue;

    public StageDiscount() {
        this.stageDiscountId = UUID.randomUUID().toString();
    }

    public static StageDiscount of(
            String companyId,
            String productionOrderId,
            ProductionOrderStage stage,
            DiscountType discountType,
            BigDecimal discountValue
    ) {
        StageDiscount discount = new StageDiscount();
        discount.companyId = companyId;
        discount.productionOrderId = productionOrderId;
        discount.stage = stage;
        discount.discountType = discountType;
        discount.discountValue = discountValue;
        return discount;
    }

    public String getStageDiscountId() {
        return stageDiscountId;
    }

    public void setStageDiscountId(String stageDiscountId) {
        this.stageDiscountId = stageDiscountId;
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

    public ProductionOrderStage getStage() {
        return stage;
    }

    public void setStage(ProductionOrderStage stage) {
        this.stage = stage;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StageDiscount that)) {
            return false;
        }
        return Objects.equals(productionOrderId, that.productionOrderId) && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(productionOrderId, stage);
    }
}
