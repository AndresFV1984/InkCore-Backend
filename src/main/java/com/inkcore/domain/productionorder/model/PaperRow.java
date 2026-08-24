package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Fila de Corte de papel ({@code indicolors.production_order_paper_rows}).
 */
public final class PaperRow {

    private String paperRowId;
    private String companyId;
    private String productionOrderId;
    private String plateId;
    private String parentRowId;

    private String cutRowKey;
    private boolean missingSupply;
    private Integer missingSheetsQuantity;

    private boolean clientSuppliesPaper;

    private String paperTypeId;
    private String paperName;
    private String paperSize;
    private BigDecimal sheetValue;
    private Integer packageUnit;
    private Boolean coated;

    private String cutLayoutId;
    private String cutLayoutName;
    private String cutLayoutSize;
    private Integer piecesPerSheet;
    private BigDecimal cutValue;

    private Boolean paperCut;
    private Integer deliveredSheetsByClient;
    private Integer manualGoodSizes;
    private Integer manualSurplus;

    private Integer calculatedSheetsCount;
    private BigDecimal totalPaperValue;
    private BigDecimal totalCutValue;

    public PaperRow() {
        this.paperRowId = UUID.randomUUID().toString();
    }

    public String getPaperRowId() {
        return paperRowId;
    }

    public void setPaperRowId(String paperRowId) {
        this.paperRowId = paperRowId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaperRow that)) {
            return false;
        }
        return Objects.equals(paperRowId, that.paperRowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paperRowId);
    }
}
