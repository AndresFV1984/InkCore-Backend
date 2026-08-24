package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Configuración de impresión de una plancha ({@code indicolors.production_order_print}).
 */
public final class PrintConfig {

    private String printId;
    private String companyId;
    private String productionOrderId;
    private String plateId;

    private Boolean clientSuppliesSherpa;
    private BigDecimal sherpaTestPrice;
    private BigDecimal machineOutputValue;
    private Map<String, Object> inkEstimation;

    private DiscountType printingDiscountType;
    private BigDecimal printingDiscountValue;
    private boolean completed;

    private List<PrintEntry> entries = new ArrayList<>();

    public PrintConfig() {
        this.printId = UUID.randomUUID().toString();
    }

    public String getPrintId() {
        return printId;
    }

    public void setPrintId(String printId) {
        this.printId = printId;
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

    public String getPlateId() {
        return plateId;
    }

    public void setPlateId(String plateId) {
        this.plateId = plateId;
    }

    public Boolean getClientSuppliesSherpa() {
        return clientSuppliesSherpa;
    }

    public void setClientSuppliesSherpa(Boolean clientSuppliesSherpa) {
        this.clientSuppliesSherpa = clientSuppliesSherpa;
    }

    public BigDecimal getSherpaTestPrice() {
        return sherpaTestPrice;
    }

    public void setSherpaTestPrice(BigDecimal sherpaTestPrice) {
        this.sherpaTestPrice = sherpaTestPrice;
    }

    public BigDecimal getMachineOutputValue() {
        return machineOutputValue;
    }

    public void setMachineOutputValue(BigDecimal machineOutputValue) {
        this.machineOutputValue = machineOutputValue;
    }

    public Map<String, Object> getInkEstimation() {
        return inkEstimation;
    }

    public void setInkEstimation(Map<String, Object> inkEstimation) {
        this.inkEstimation = inkEstimation == null ? null : new LinkedHashMap<>(inkEstimation);
    }

    public DiscountType getPrintingDiscountType() {
        return printingDiscountType;
    }

    public void setPrintingDiscountType(DiscountType printingDiscountType) {
        this.printingDiscountType = printingDiscountType;
    }

    public BigDecimal getPrintingDiscountValue() {
        return printingDiscountValue;
    }

    public void setPrintingDiscountValue(BigDecimal printingDiscountValue) {
        this.printingDiscountValue = printingDiscountValue;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public List<PrintEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<PrintEntry> entries) {
        this.entries = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PrintConfig that)) {
            return false;
        }
        return Objects.equals(printId, that.printId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(printId);
    }
}
