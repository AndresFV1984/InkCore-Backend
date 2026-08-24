package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entrada de tiro/retiro ({@code indicolors.production_order_print_entries}).
 */
public final class PrintEntry {

    private String printEntryId;
    private String companyId;
    private String printId;

    private int shotsInkCount;
    private List<String> shotsInks = new ArrayList<>();
    private int reverseInkCount;
    private List<String> reverseInks = new ArrayList<>();

    private FlipType basicFlipType = FlipType.NO_FLIP;
    private String basicThousandRateId;
    private String basicRateName;
    private BigDecimal basicRatePrice;
    private BigDecimal basicRateGripperFlipPrice;
    private BigDecimal basicRateSquareFlipPrice;
    private BigDecimal basicCalculatedThousands;
    private BigDecimal basicPrintingPrice;

    private FlipType pantoneFlipType = FlipType.NO_FLIP;
    private Boolean clientSuppliesPantoneInk;
    private BigDecimal pantoneInkChargePrice;
    private String pantoneThousandRateId;
    private String pantoneRateName;
    private BigDecimal pantoneRatePrice;
    private BigDecimal pantoneRateGripperFlipPrice;
    private BigDecimal pantoneRateSquareFlipPrice;
    private BigDecimal pantoneCalculatedThousands;
    private BigDecimal pantonePrintingPrice;

    public PrintEntry() {
        this.printEntryId = UUID.randomUUID().toString();
    }

    public String getPrintEntryId() {
        return printEntryId;
    }

    public void setPrintEntryId(String printEntryId) {
        this.printEntryId = printEntryId;
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
        this.shotsInks = shotsInks == null ? new ArrayList<>() : new ArrayList<>(shotsInks);
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
        this.reverseInks = reverseInks == null ? new ArrayList<>() : new ArrayList<>(reverseInks);
    }

    public FlipType getBasicFlipType() {
        return basicFlipType;
    }

    public void setBasicFlipType(FlipType basicFlipType) {
        this.basicFlipType = basicFlipType == null ? FlipType.NO_FLIP : basicFlipType;
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

    public FlipType getPantoneFlipType() {
        return pantoneFlipType;
    }

    public void setPantoneFlipType(FlipType pantoneFlipType) {
        this.pantoneFlipType = pantoneFlipType == null ? FlipType.NO_FLIP : pantoneFlipType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PrintEntry that)) {
            return false;
        }
        return Objects.equals(printEntryId, that.printEntryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(printEntryId);
    }
}
