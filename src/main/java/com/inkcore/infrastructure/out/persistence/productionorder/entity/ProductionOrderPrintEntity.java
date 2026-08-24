package com.inkcore.infrastructure.out.persistence.productionorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "production_order_print", schema = "indicolors")
public class ProductionOrderPrintEntity implements Persistable<String> {

    @Id
    @Column(name = "production_order_print_id", length = 64)
    private String productionOrderPrintId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(name = "plate_id", nullable = false, length = 64)
    private String plateId;

    @Column(name = "client_supplies_sherpa")
    private Boolean clientSuppliesSherpa;

    @Column(name = "sherpa_test_price", precision = 12, scale = 2)
    private BigDecimal sherpaTestPrice;

    @Column(name = "machine_output_value", precision = 12, scale = 2)
    private BigDecimal machineOutputValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ink_estimation")
    private Map<String, Object> inkEstimation;

    @Column(name = "printing_discount_type", length = 10)
    private String printingDiscountType;

    @Column(name = "printing_discount_value", precision = 12, scale = 2)
    private BigDecimal printingDiscountValue;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProductionOrderPrintEntity() {
    }

    @Override
    public String getId() {
        return productionOrderPrintId;
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

    public String getProductionOrderPrintId() {
        return productionOrderPrintId;
    }

    public void setProductionOrderPrintId(String productionOrderPrintId) {
        this.productionOrderPrintId = productionOrderPrintId;
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
        this.inkEstimation = inkEstimation;
    }

    public String getPrintingDiscountType() {
        return printingDiscountType;
    }

    public void setPrintingDiscountType(String printingDiscountType) {
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
