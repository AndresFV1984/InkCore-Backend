package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Etapa tercerizada ({@code indicolors.production_order_outsourced_stages}).
 * Cobro al cliente = {@link #resolveSellPrice()}.
 * Ciclo: PENDING → DISPATCHED → IN_PROGRESS → COMPLETED.
 */
public final class OutsourcedStage {

    private static final int MONEY_SCALE = 2;

    private String outsourcedStageId;
    private String companyId;
    private String productionOrderId;
    private ProductionOrderStage stage;
    private boolean enabled;
    private BigDecimal vendorCost;
    private BigDecimal markupPercent;
    private BigDecimal sellPrice;
    private String supplierId;
    private String supplierName;

    private LocalDate quotedAt;
    private int quantity;
    private String costResponsibleUserId;
    private String costResponsibleUserName;
    private LocalDate quoteValidUntil;

    private LocalDate sentAt;
    private String sentDescription;
    private LocalDate expectedReturnAt;

    private LocalDate receivedAt;
    private String receivedByUserId;
    private OutsourcedFulfillmentStatus fulfillmentStatus;

    public OutsourcedStage() {
        this.outsourcedStageId = UUID.randomUUID().toString();
        this.enabled = true;
        this.vendorCost = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.markupPercent = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.sellPrice = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.quantity = 0;
        this.fulfillmentStatus = OutsourcedFulfillmentStatus.PENDING;
    }

    public static OutsourcedStage of(
            String companyId,
            String productionOrderId,
            ProductionOrderStage stage,
            boolean enabled,
            BigDecimal vendorCost,
            BigDecimal markupPercent,
            BigDecimal sellPrice,
            String supplierId,
            String supplierName,
            LocalDate quotedAt,
            Integer quantity,
            String costResponsibleUserId,
            String costResponsibleUserName,
            LocalDate quoteValidUntil,
            LocalDate sentAt,
            String sentDescription,
            LocalDate expectedReturnAt,
            LocalDate receivedAt,
            String receivedByUserId,
            OutsourcedFulfillmentStatus fulfillmentStatus
    ) {
        OutsourcedStage row = new OutsourcedStage();
        row.companyId = companyId;
        row.productionOrderId = productionOrderId;
        row.stage = stage;
        row.enabled = enabled;
        row.vendorCost = money(vendorCost);
        row.markupPercent = percent(markupPercent);
        row.sellPrice = sellPrice != null ? money(sellPrice) : row.computeSellPrice();
        row.supplierId = blankToNull(supplierId);
        row.supplierName = blankToNull(supplierName);
        row.quotedAt = quotedAt;
        row.quantity = quantity == null || quantity < 0 ? 0 : quantity;
        row.costResponsibleUserId = blankToNull(costResponsibleUserId);
        row.costResponsibleUserName = blankToNull(costResponsibleUserName);
        row.quoteValidUntil = quoteValidUntil;
        row.sentAt = sentAt;
        row.sentDescription = blankToNull(sentDescription);
        row.expectedReturnAt = expectedReturnAt;
        row.receivedAt = receivedAt;
        row.receivedByUserId = blankToNull(receivedByUserId);
        row.fulfillmentStatus = OutsourcedFulfillmentStatus.resolve(
                fulfillmentStatus,
                receivedAt,
                sentAt
        );
        if (!enabled) {
            row.fulfillmentStatus = OutsourcedFulfillmentStatus.PENDING;
        }
        return row;
    }

    public BigDecimal resolveSellPrice() {
        if (sellPrice != null && sellPrice.signum() > 0) {
            return money(sellPrice);
        }
        return computeSellPrice();
    }

    public BigDecimal computeSellPrice() {
        BigDecimal cost = money(vendorCost);
        if (cost.signum() <= 0) {
            return money(BigDecimal.ZERO);
        }
        BigDecimal markup = percent(markupPercent);
        BigDecimal factor = BigDecimal.ONE.add(
                markup.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
        );
        return money(cost.multiply(factor));
    }

    /** True cuando la etapa ya no bloquea estación (trabajo del proveedor finalizado). */
    public boolean isFulfillmentComplete() {
        return enabled && fulfillmentStatus == OutsourcedFulfillmentStatus.COMPLETED;
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getOutsourcedStageId() {
        return outsourcedStageId;
    }

    public void setOutsourcedStageId(String outsourcedStageId) {
        this.outsourcedStageId = outsourcedStageId;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getVendorCost() {
        return vendorCost;
    }

    public void setVendorCost(BigDecimal vendorCost) {
        this.vendorCost = money(vendorCost);
    }

    public BigDecimal getMarkupPercent() {
        return markupPercent;
    }

    public void setMarkupPercent(BigDecimal markupPercent) {
        this.markupPercent = percent(markupPercent);
    }

    public BigDecimal getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(BigDecimal sellPrice) {
        this.sellPrice = money(sellPrice);
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = blankToNull(supplierId);
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = blankToNull(supplierName);
    }

    public LocalDate getQuotedAt() {
        return quotedAt;
    }

    public void setQuotedAt(LocalDate quotedAt) {
        this.quotedAt = quotedAt;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(0, quantity);
    }

    public String getCostResponsibleUserId() {
        return costResponsibleUserId;
    }

    public void setCostResponsibleUserId(String costResponsibleUserId) {
        this.costResponsibleUserId = blankToNull(costResponsibleUserId);
    }

    public String getCostResponsibleUserName() {
        return costResponsibleUserName;
    }

    public void setCostResponsibleUserName(String costResponsibleUserName) {
        this.costResponsibleUserName = blankToNull(costResponsibleUserName);
    }

    public LocalDate getQuoteValidUntil() {
        return quoteValidUntil;
    }

    public void setQuoteValidUntil(LocalDate quoteValidUntil) {
        this.quoteValidUntil = quoteValidUntil;
    }

    public LocalDate getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDate sentAt) {
        this.sentAt = sentAt;
    }

    public String getSentDescription() {
        return sentDescription;
    }

    public void setSentDescription(String sentDescription) {
        this.sentDescription = blankToNull(sentDescription);
    }

    public LocalDate getExpectedReturnAt() {
        return expectedReturnAt;
    }

    public void setExpectedReturnAt(LocalDate expectedReturnAt) {
        this.expectedReturnAt = expectedReturnAt;
    }

    public LocalDate getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDate receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getReceivedByUserId() {
        return receivedByUserId;
    }

    public void setReceivedByUserId(String receivedByUserId) {
        this.receivedByUserId = blankToNull(receivedByUserId);
    }

    public OutsourcedFulfillmentStatus getFulfillmentStatus() {
        return fulfillmentStatus == null ? OutsourcedFulfillmentStatus.PENDING : fulfillmentStatus;
    }

    public void setFulfillmentStatus(OutsourcedFulfillmentStatus fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus == null
                ? OutsourcedFulfillmentStatus.PENDING
                : fulfillmentStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OutsourcedStage that)) {
            return false;
        }
        return Objects.equals(productionOrderId, that.productionOrderId) && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(productionOrderId, stage);
    }
}
