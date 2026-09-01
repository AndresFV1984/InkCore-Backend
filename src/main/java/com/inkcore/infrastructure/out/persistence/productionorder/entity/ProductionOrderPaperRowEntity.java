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
@Table(name = "production_order_paper_rows", schema = "indicolors")
public class ProductionOrderPaperRowEntity implements Persistable<String> {

    @Id
    @Column(name = "production_order_paper_row_id", length = 64)
    private String productionOrderPaperRowId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(name = "plate_id", nullable = false, length = 64)
    private String plateId;

    @Column(name = "parent_row_id", length = 64)
    private String parentRowId;

    @Column(name = "cut_row_key", nullable = false, length = 50)
    private String cutRowKey;

    @Column(name = "is_missing_supply", nullable = false)
    private boolean missingSupply;

    @Column(name = "missing_sheets_quantity")
    private Integer missingSheetsQuantity;

    @Column(name = "client_supplies_paper", nullable = false)
    private boolean clientSuppliesPaper;

    @Column(name = "paper_type_id", length = 64)
    private String paperTypeId;

    @Column(name = "supplier_id", length = 64)
    private String supplierId;

    @Column(name = "paper_name", length = 80)
    private String paperName;

    @Column(name = "paper_size", length = 30)
    private String paperSize;

    @Column(name = "sheet_value", precision = 12, scale = 2)
    private BigDecimal sheetValue;

    @Column(name = "package_unit")
    private Integer packageUnit;

    @Column(name = "is_coated")
    private Boolean coated;

    @Column(name = "cut_layout_id", length = 64)
    private String cutLayoutId;

    @Column(name = "cut_layout_name", length = 80)
    private String cutLayoutName;

    @Column(name = "cut_layout_size", length = 30)
    private String cutLayoutSize;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "pieces_per_sheet")
    private Integer piecesPerSheet;

    @Column(name = "cut_value", precision = 12, scale = 2)
    private BigDecimal cutValue;

    @Column(name = "is_paper_cut")
    private Boolean paperCut;

    @Column(name = "delivered_sheets_by_client")
    private Integer deliveredSheetsByClient;

    @Column(name = "manual_good_sizes")
    private Integer manualGoodSizes;

    @Column(name = "manual_surplus")
    private Integer manualSurplus;

    @Column(name = "calculated_sheets_count")
    private Integer calculatedSheetsCount;

    @Column(name = "total_paper_value", precision = 12, scale = 2)
    private BigDecimal totalPaperValue;

    @Column(name = "total_cut_value", precision = 12, scale = 2)
    private BigDecimal totalCutValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProductionOrderPaperRowEntity() {
    }

    @Override
    public String getId() {
        return productionOrderPaperRowId;
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

    public String getProductionOrderPaperRowId() {
        return productionOrderPaperRowId;
    }

    public void setProductionOrderPaperRowId(String productionOrderPaperRowId) {
        this.productionOrderPaperRowId = productionOrderPaperRowId;
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

    public String getParentRowId() {
        return parentRowId;
    }

    public void setParentRowId(String parentRowId) {
        this.parentRowId = parentRowId;
    }

    public String getCutRowKey() {
        return cutRowKey;
    }

    public void setCutRowKey(String cutRowKey) {
        this.cutRowKey = cutRowKey;
    }

    public boolean isMissingSupply() {
        return missingSupply;
    }

    public void setMissingSupply(boolean missingSupply) {
        this.missingSupply = missingSupply;
    }

    public Integer getMissingSheetsQuantity() {
        return missingSheetsQuantity;
    }

    public void setMissingSheetsQuantity(Integer missingSheetsQuantity) {
        this.missingSheetsQuantity = missingSheetsQuantity;
    }

    public boolean isClientSuppliesPaper() {
        return clientSuppliesPaper;
    }

    public void setClientSuppliesPaper(boolean clientSuppliesPaper) {
        this.clientSuppliesPaper = clientSuppliesPaper;
    }

    public String getPaperTypeId() {
        return paperTypeId;
    }

    public void setPaperTypeId(String paperTypeId) {
        this.paperTypeId = paperTypeId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getPaperName() {
        return paperName;
    }

    public void setPaperName(String paperName) {
        this.paperName = paperName;
    }

    public String getPaperSize() {
        return paperSize;
    }

    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    public BigDecimal getSheetValue() {
        return sheetValue;
    }

    public void setSheetValue(BigDecimal sheetValue) {
        this.sheetValue = sheetValue;
    }

    public Integer getPackageUnit() {
        return packageUnit;
    }

    public void setPackageUnit(Integer packageUnit) {
        this.packageUnit = packageUnit;
    }

    public Boolean getCoated() {
        return coated;
    }

    public void setCoated(Boolean coated) {
        this.coated = coated;
    }

    public String getCutLayoutId() {
        return cutLayoutId;
    }

    public void setCutLayoutId(String cutLayoutId) {
        this.cutLayoutId = cutLayoutId;
    }

    public String getCutLayoutName() {
        return cutLayoutName;
    }

    public void setCutLayoutName(String cutLayoutName) {
        this.cutLayoutName = cutLayoutName;
    }

    public String getCutLayoutSize() {
        return cutLayoutSize;
    }

    public void setCutLayoutSize(String cutLayoutSize) {
        this.cutLayoutSize = cutLayoutSize;
    }

    public Integer getPiecesPerSheet() {
        return piecesPerSheet;
    }

    public void setPiecesPerSheet(Integer piecesPerSheet) {
        this.piecesPerSheet = piecesPerSheet;
    }

    public BigDecimal getCutValue() {
        return cutValue;
    }

    public void setCutValue(BigDecimal cutValue) {
        this.cutValue = cutValue;
    }

    public Boolean getPaperCut() {
        return paperCut;
    }

    public void setPaperCut(Boolean paperCut) {
        this.paperCut = paperCut;
    }

    public Integer getDeliveredSheetsByClient() {
        return deliveredSheetsByClient;
    }

    public void setDeliveredSheetsByClient(Integer deliveredSheetsByClient) {
        this.deliveredSheetsByClient = deliveredSheetsByClient;
    }

    public Integer getManualGoodSizes() {
        return manualGoodSizes;
    }

    public void setManualGoodSizes(Integer manualGoodSizes) {
        this.manualGoodSizes = manualGoodSizes;
    }

    public Integer getManualSurplus() {
        return manualSurplus;
    }

    public void setManualSurplus(Integer manualSurplus) {
        this.manualSurplus = manualSurplus;
    }

    public Integer getCalculatedSheetsCount() {
        return calculatedSheetsCount;
    }

    public void setCalculatedSheetsCount(Integer calculatedSheetsCount) {
        this.calculatedSheetsCount = calculatedSheetsCount;
    }

    public BigDecimal getTotalPaperValue() {
        return totalPaperValue;
    }

    public void setTotalPaperValue(BigDecimal totalPaperValue) {
        this.totalPaperValue = totalPaperValue;
    }

    public BigDecimal getTotalCutValue() {
        return totalCutValue;
    }

    public void setTotalCutValue(BigDecimal totalCutValue) {
        this.totalCutValue = totalCutValue;
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
