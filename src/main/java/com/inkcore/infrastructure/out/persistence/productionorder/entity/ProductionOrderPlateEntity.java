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

@Entity
@Table(name = "production_order_plates", schema = "indicolors")
public class ProductionOrderPlateEntity implements Persistable<String> {

    @Id
    @Column(name = "production_order_plate_id", length = 64)
    private String productionOrderPlateId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(name = "colors", nullable = false, length = 20)
    private String colors;

    @Column(name = "plate_type_id", length = 64)
    private String plateTypeId;

    @Column(name = "plate_name", length = 80)
    private String plateName;

    @Column(name = "plate_size", length = 30)
    private String plateSize;

    @Column(name = "plate_price", precision = 12, scale = 2)
    private BigDecimal platePrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "cavities")
    private Integer cavities;

    @Column(name = "good_sizes")
    private Integer goodSizes;

    @Column(name = "surplus")
    private Integer surplus;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "plates_count")
    private Integer platesCount;

    @Column(name = "total_value", precision = 12, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "detail", length = 100)
    private String detail;

    @Column(name = "observation")
    private String observation;

    @Column(name = "manual_entry", nullable = false)
    private boolean manualEntry;

    @Column(name = "plate_supply", length = 20)
    private String plateSupply;

    @Column(name = "plate_replacement", nullable = false)
    private boolean plateReplacement;

    @Column(name = "replacement_quantity")
    private Integer replacementQuantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProductionOrderPlateEntity() {
    }

    @Override
    public String getId() {
        return productionOrderPlateId;
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

    public String getProductionOrderPlateId() {
        return productionOrderPlateId;
    }

    public void setProductionOrderPlateId(String productionOrderPlateId) {
        this.productionOrderPlateId = productionOrderPlateId;
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
