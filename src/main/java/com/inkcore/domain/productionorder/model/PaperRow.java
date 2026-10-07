package com.inkcore.domain.productionorder.model;

import com.inkcore.domain.paper.model.PriceRule;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    private String paperId;
    private String supplierId;
    private String paperName;
    private String paperSize;
    private BigDecimal sheetValue;
    private Integer packageUnit;
    private Boolean coated;
    private BigDecimal freightPerSheetSnapshot;
    private LocalDate priceDateSnapshot;
    private PriceRule priceRule;
    private Integer piecesPerSheetSnapshot;
    private BigDecimal netSheets;
    private BigDecimal wasteSheets;
    private BigDecimal totalSheets;
    private BigDecimal costPerPiece;
    private Boolean coatedSnapshot;

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
    private BigDecimal plannedWastePercentage;

    /** Remanente usado como origen del corte (opcional). */
    private String paperRemnantId;
    /** Unidades descontadas de {@code paper_remnants.quantity_available}. */
    private BigDecimal remnantQuantityUsed;

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

    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(String paperId) {
        this.paperId = paperId;
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

    public BigDecimal getFreightPerSheetSnapshot() {
        return freightPerSheetSnapshot;
    }

    public void setFreightPerSheetSnapshot(BigDecimal freightPerSheetSnapshot) {
        this.freightPerSheetSnapshot = freightPerSheetSnapshot;
    }

    public LocalDate getPriceDateSnapshot() {
        return priceDateSnapshot;
    }

    public void setPriceDateSnapshot(LocalDate priceDateSnapshot) {
        this.priceDateSnapshot = priceDateSnapshot;
    }

    public PriceRule getPriceRule() {
        return priceRule;
    }

    public void setPriceRule(PriceRule priceRule) {
        this.priceRule = priceRule;
    }

    public Integer getPiecesPerSheetSnapshot() {
        return piecesPerSheetSnapshot;
    }

    public void setPiecesPerSheetSnapshot(Integer piecesPerSheetSnapshot) {
        this.piecesPerSheetSnapshot = piecesPerSheetSnapshot;
    }

    public BigDecimal getNetSheets() {
        return netSheets;
    }

    public void setNetSheets(BigDecimal netSheets) {
        this.netSheets = netSheets;
    }

    public BigDecimal getWasteSheets() {
        return wasteSheets;
    }

    public void setWasteSheets(BigDecimal wasteSheets) {
        this.wasteSheets = wasteSheets;
    }

    public BigDecimal getTotalSheets() {
        return totalSheets;
    }

    public void setTotalSheets(BigDecimal totalSheets) {
        this.totalSheets = totalSheets;
    }

    public BigDecimal getCostPerPiece() {
        return costPerPiece;
    }

    public void setCostPerPiece(BigDecimal costPerPiece) {
        this.costPerPiece = costPerPiece;
    }

    public Boolean getCoatedSnapshot() {
        return coatedSnapshot;
    }

    public void setCoatedSnapshot(Boolean coatedSnapshot) {
        this.coatedSnapshot = coatedSnapshot;
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

    public BigDecimal getPlannedWastePercentage() {
        return plannedWastePercentage;
    }

    public void setPlannedWastePercentage(BigDecimal plannedWastePercentage) {
        this.plannedWastePercentage = plannedWastePercentage;
    }

    public String getPaperRemnantId() {
        return paperRemnantId;
    }

    public void setPaperRemnantId(String paperRemnantId) {
        this.paperRemnantId = paperRemnantId;
    }

    public BigDecimal getRemnantQuantityUsed() {
        return remnantQuantityUsed;
    }

    public void setRemnantQuantityUsed(BigDecimal remnantQuantityUsed) {
        this.remnantQuantityUsed = remnantQuantityUsed;
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
