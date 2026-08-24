package com.inkcore.infrastructure.out.persistence.thousandrate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "thousand_rates", schema = "indicolors")
public class ThousandRateEntity implements Persistable<String> {

    @Id
    @Column(name = "thousand_rate_id", length = 64)
    private String thousandRateId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "color_category", nullable = false, length = 50)
    private String colorCategory;

    @Column(name = "thousand_unit", nullable = false)
    private int thousandUnit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean state;

    @Column(name = "min_threshold_units", nullable = false)
    private int minThresholdUnits;

    @Column(name = "min_thousand", nullable = false, precision = 10, scale = 2)
    private BigDecimal minThousand;

    @Column(name = "decimal_threshold", nullable = false, precision = 3, scale = 2)
    private BigDecimal decimalThreshold;

    @Column(name = "gripper_flip_price", precision = 12, scale = 2)
    private BigDecimal gripperFlipPrice;

    @Column(name = "square_flip_price", precision = 12, scale = 2)
    private BigDecimal squareFlipPrice;

    @Column(name = "is_default", nullable = false)
    private boolean defaultFlag;

    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    public ThousandRateEntity() {
    }

    @Override
    public String getId() {
        return thousandRateId;
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

    public String getThousandRateId() {
        return thousandRateId;
    }

    public void setThousandRateId(String thousandRateId) {
        this.thousandRateId = thousandRateId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorCategory() {
        return colorCategory;
    }

    public void setColorCategory(String colorCategory) {
        this.colorCategory = colorCategory;
    }

    public int getThousandUnit() {
        return thousandUnit;
    }

    public void setThousandUnit(int thousandUnit) {
        this.thousandUnit = thousandUnit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public int getMinThresholdUnits() {
        return minThresholdUnits;
    }

    public void setMinThresholdUnits(int minThresholdUnits) {
        this.minThresholdUnits = minThresholdUnits;
    }

    public BigDecimal getMinThousand() {
        return minThousand;
    }

    public void setMinThousand(BigDecimal minThousand) {
        this.minThousand = minThousand;
    }

    public BigDecimal getDecimalThreshold() {
        return decimalThreshold;
    }

    public void setDecimalThreshold(BigDecimal decimalThreshold) {
        this.decimalThreshold = decimalThreshold;
    }

    public BigDecimal getGripperFlipPrice() {
        return gripperFlipPrice;
    }

    public void setGripperFlipPrice(BigDecimal gripperFlipPrice) {
        this.gripperFlipPrice = gripperFlipPrice;
    }

    public BigDecimal getSquareFlipPrice() {
        return squareFlipPrice;
    }

    public void setSquareFlipPrice(BigDecimal squareFlipPrice) {
        this.squareFlipPrice = squareFlipPrice;
    }

    public boolean isDefault() {
        return defaultFlag;
    }

    public void setDefault(boolean defaultFlag) {
        this.defaultFlag = defaultFlag;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }
}
