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
import java.util.List;

@Entity
@Table(name = "production_order_print_entries", schema = "indicolors")
public class ProductionOrderPrintEntryEntity implements Persistable<String> {

    @Id
    @Column(name = "production_order_print_entry_id", length = 64)
    private String productionOrderPrintEntryId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "print_id", nullable = false, length = 64)
    private String printId;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "shots_ink_count", nullable = false)
    private int shotsInkCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shots_inks", nullable = false)
    private List<String> shotsInks;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "reverse_ink_count", nullable = false)
    private int reverseInkCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reverse_inks", nullable = false)
    private List<String> reverseInks;

    @Column(name = "basic_flip_type", nullable = false, length = 20)
    private String basicFlipType;

    @Column(name = "basic_thousand_rate_id", length = 64)
    private String basicThousandRateId;

    @Column(name = "basic_rate_name", length = 150)
    private String basicRateName;

    @Column(name = "basic_rate_price", precision = 12, scale = 2)
    private BigDecimal basicRatePrice;

    @Column(name = "basic_rate_gripper_flip_price", precision = 12, scale = 2)
    private BigDecimal basicRateGripperFlipPrice;

    @Column(name = "basic_rate_square_flip_price", precision = 12, scale = 2)
    private BigDecimal basicRateSquareFlipPrice;

    @Column(name = "basic_calculated_thousands", precision = 10, scale = 2)
    private BigDecimal basicCalculatedThousands;

    @Column(name = "basic_printing_price", precision = 12, scale = 2)
    private BigDecimal basicPrintingPrice;

    @Column(name = "pantone_flip_type", nullable = false, length = 20)
    private String pantoneFlipType;

    @Column(name = "client_supplies_pantone_ink")
    private Boolean clientSuppliesPantoneInk;

    @Column(name = "pantone_ink_charge_price", precision = 12, scale = 2)
    private BigDecimal pantoneInkChargePrice;

    @Column(name = "pantone_thousand_rate_id", length = 64)
    private String pantoneThousandRateId;

    @Column(name = "pantone_rate_name", length = 150)
    private String pantoneRateName;

    @Column(name = "pantone_rate_price", precision = 12, scale = 2)
    private BigDecimal pantoneRatePrice;

    @Column(name = "pantone_rate_gripper_flip_price", precision = 12, scale = 2)
    private BigDecimal pantoneRateGripperFlipPrice;

    @Column(name = "pantone_rate_square_flip_price", precision = 12, scale = 2)
    private BigDecimal pantoneRateSquareFlipPrice;

    @Column(name = "pantone_calculated_thousands", precision = 10, scale = 2)
    private BigDecimal pantoneCalculatedThousands;

    @Column(name = "pantone_printing_price", precision = 12, scale = 2)
    private BigDecimal pantonePrintingPrice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ProductionOrderPrintEntryEntity() {
    }

    @Override
    public String getId() {
        return productionOrderPrintEntryId;
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

    public String getProductionOrderPrintEntryId() {
        return productionOrderPrintEntryId;
    }

    public void setProductionOrderPrintEntryId(String productionOrderPrintEntryId) {
        this.productionOrderPrintEntryId = productionOrderPrintEntryId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getPrintId() {
        return printId;
    }

    public void setPrintId(String printId) {
        this.printId = printId;
    }

    public int getShotsInkCount() {
        return shotsInkCount;
    }

    public void setShotsInkCount(int shotsInkCount) {
        this.shotsInkCount = shotsInkCount;
    }

    public List<String> getShotsInks() {
        return shotsInks;
    }

    public void setShotsInks(List<String> shotsInks) {
        this.shotsInks = shotsInks;
    }

    public int getReverseInkCount() {
        return reverseInkCount;
    }

    public void setReverseInkCount(int reverseInkCount) {
        this.reverseInkCount = reverseInkCount;
    }

    public List<String> getReverseInks() {
        return reverseInks;
    }

    public void setReverseInks(List<String> reverseInks) {
        this.reverseInks = reverseInks;
    }

    public String getBasicFlipType() {
        return basicFlipType;
    }

    public void setBasicFlipType(String basicFlipType) {
        this.basicFlipType = basicFlipType;
    }

    public String getBasicThousandRateId() {
        return basicThousandRateId;
    }

    public void setBasicThousandRateId(String basicThousandRateId) {
        this.basicThousandRateId = basicThousandRateId;
    }

    public String getBasicRateName() {
        return basicRateName;
    }

    public void setBasicRateName(String basicRateName) {
        this.basicRateName = basicRateName;
    }

    public BigDecimal getBasicRatePrice() {
        return basicRatePrice;
    }

    public void setBasicRatePrice(BigDecimal basicRatePrice) {
        this.basicRatePrice = basicRatePrice;
    }

    public BigDecimal getBasicRateGripperFlipPrice() {
        return basicRateGripperFlipPrice;
    }

    public void setBasicRateGripperFlipPrice(BigDecimal basicRateGripperFlipPrice) {
        this.basicRateGripperFlipPrice = basicRateGripperFlipPrice;
    }

    public BigDecimal getBasicRateSquareFlipPrice() {
        return basicRateSquareFlipPrice;
    }

    public void setBasicRateSquareFlipPrice(BigDecimal basicRateSquareFlipPrice) {
        this.basicRateSquareFlipPrice = basicRateSquareFlipPrice;
    }

    public BigDecimal getBasicCalculatedThousands() {
        return basicCalculatedThousands;
    }

    public void setBasicCalculatedThousands(BigDecimal basicCalculatedThousands) {
        this.basicCalculatedThousands = basicCalculatedThousands;
    }

    public BigDecimal getBasicPrintingPrice() {
        return basicPrintingPrice;
    }

    public void setBasicPrintingPrice(BigDecimal basicPrintingPrice) {
        this.basicPrintingPrice = basicPrintingPrice;
    }

    public String getPantoneFlipType() {
        return pantoneFlipType;
    }

    public void setPantoneFlipType(String pantoneFlipType) {
        this.pantoneFlipType = pantoneFlipType;
    }

    public Boolean getClientSuppliesPantoneInk() {
        return clientSuppliesPantoneInk;
    }

    public void setClientSuppliesPantoneInk(Boolean clientSuppliesPantoneInk) {
        this.clientSuppliesPantoneInk = clientSuppliesPantoneInk;
    }

    public BigDecimal getPantoneInkChargePrice() {
        return pantoneInkChargePrice;
    }

    public void setPantoneInkChargePrice(BigDecimal pantoneInkChargePrice) {
        this.pantoneInkChargePrice = pantoneInkChargePrice;
    }

    public String getPantoneThousandRateId() {
        return pantoneThousandRateId;
    }

    public void setPantoneThousandRateId(String pantoneThousandRateId) {
        this.pantoneThousandRateId = pantoneThousandRateId;
    }

    public String getPantoneRateName() {
        return pantoneRateName;
    }

    public void setPantoneRateName(String pantoneRateName) {
        this.pantoneRateName = pantoneRateName;
    }

    public BigDecimal getPantoneRatePrice() {
        return pantoneRatePrice;
    }

    public void setPantoneRatePrice(BigDecimal pantoneRatePrice) {
        this.pantoneRatePrice = pantoneRatePrice;
    }

    public BigDecimal getPantoneRateGripperFlipPrice() {
        return pantoneRateGripperFlipPrice;
    }

    public void setPantoneRateGripperFlipPrice(BigDecimal pantoneRateGripperFlipPrice) {
        this.pantoneRateGripperFlipPrice = pantoneRateGripperFlipPrice;
    }

    public BigDecimal getPantoneRateSquareFlipPrice() {
        return pantoneRateSquareFlipPrice;
    }

    public void setPantoneRateSquareFlipPrice(BigDecimal pantoneRateSquareFlipPrice) {
        this.pantoneRateSquareFlipPrice = pantoneRateSquareFlipPrice;
    }

    public BigDecimal getPantoneCalculatedThousands() {
        return pantoneCalculatedThousands;
    }

    public void setPantoneCalculatedThousands(BigDecimal pantoneCalculatedThousands) {
        this.pantoneCalculatedThousands = pantoneCalculatedThousands;
    }

    public BigDecimal getPantonePrintingPrice() {
        return pantonePrintingPrice;
    }

    public void setPantonePrintingPrice(BigDecimal pantonePrintingPrice) {
        this.pantonePrintingPrice = pantonePrintingPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
