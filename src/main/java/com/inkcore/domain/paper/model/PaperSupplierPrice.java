package com.inkcore.domain.paper.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Precio vigente de un papel por proveedor.
 */
public final class PaperSupplierPrice {

    private final String paperSupplierPriceId;
    private final String companyId;
    private final String paperId;
    private final String supplierId;
    private final BigDecimal sheetValue;
    private final int packageUnit;
    private final BigDecimal freightPerSheet;
    private final Integer minPurchaseSheets;
    private final Integer paymentDays;
    private final Integer deliveryDays;
    private final LocalDate priceDate;
    private final boolean preferred;
    private final boolean state;
    private final BigDecimal landedCostPerSheet;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private PaperSupplierPrice(
            String paperSupplierPriceId,
            String companyId,
            String paperId,
            String supplierId,
            BigDecimal sheetValue,
            int packageUnit,
            BigDecimal freightPerSheet,
            Integer minPurchaseSheets,
            Integer paymentDays,
            Integer deliveryDays,
            LocalDate priceDate,
            boolean preferred,
            boolean state,
            BigDecimal landedCostPerSheet,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.paperSupplierPriceId = paperSupplierPriceId;
        this.companyId = companyId;
        this.paperId = paperId;
        this.supplierId = supplierId;
        this.sheetValue = sheetValue;
        this.packageUnit = packageUnit;
        this.freightPerSheet = freightPerSheet;
        this.minPurchaseSheets = minPurchaseSheets;
        this.paymentDays = paymentDays;
        this.deliveryDays = deliveryDays;
        this.priceDate = priceDate;
        this.preferred = preferred;
        this.state = state;
        this.landedCostPerSheet = landedCostPerSheet;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaperSupplierPrice createNew(
            String companyId,
            String paperId,
            String supplierId,
            BigDecimal sheetValue,
            Integer packageUnit,
            BigDecimal freightPerSheet,
            Integer minPurchaseSheets,
            Integer paymentDays,
            Integer deliveryDays,
            LocalDate priceDate,
            boolean preferred,
            boolean state,
            LocalDateTime now
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(paperId, "El papel es obligatorio");
        requireNotBlank(supplierId, "El proveedor es obligatorio");
        Objects.requireNonNull(priceDate, "La fecha de precio es obligatoria");
        return new PaperSupplierPrice(
                UUID.randomUUID().toString(),
                companyId.trim(),
                paperId.trim(),
                supplierId.trim(),
                positiveMoney(sheetValue, "El valor del pliego debe ser mayor que 0"),
                requirePositivePackageUnit(packageUnit),
                nonNegativeMoney(freightPerSheet),
                positiveOptionalInt(minPurchaseSheets, "El mínimo de compra debe ser mayor que 0"),
                nonNegativeOptionalInt(paymentDays),
                nonNegativeOptionalInt(deliveryDays),
                priceDate,
                preferred,
                state,
                null,
                now,
                now
        );
    }

    public PaperSupplierPrice withPricing(
            BigDecimal sheetValue,
            Integer packageUnit,
            BigDecimal freightPerSheet,
            Integer minPurchaseSheets,
            Integer paymentDays,
            Integer deliveryDays,
            LocalDate priceDate,
            boolean preferred,
            boolean state,
            LocalDateTime updatedAt
    ) {
        return new PaperSupplierPrice(
                paperSupplierPriceId,
                companyId,
                paperId,
                supplierId,
                positiveMoney(sheetValue, "El valor del pliego debe ser mayor que 0"),
                requirePositivePackageUnit(packageUnit),
                nonNegativeMoney(freightPerSheet),
                positiveOptionalInt(minPurchaseSheets, "El mínimo de compra debe ser mayor que 0"),
                nonNegativeOptionalInt(paymentDays),
                nonNegativeOptionalInt(deliveryDays),
                priceDate,
                preferred,
                state,
                landedCostPerSheet,
                createdAt,
                updatedAt
        );
    }

    public static PaperSupplierPrice reconstitute(
            String paperSupplierPriceId,
            String companyId,
            String paperId,
            String supplierId,
            BigDecimal sheetValue,
            int packageUnit,
            BigDecimal freightPerSheet,
            Integer minPurchaseSheets,
            Integer paymentDays,
            Integer deliveryDays,
            LocalDate priceDate,
            boolean preferred,
            boolean state,
            BigDecimal landedCostPerSheet,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new PaperSupplierPrice(
                paperSupplierPriceId,
                companyId,
                paperId,
                supplierId,
                sheetValue,
                packageUnit,
                freightPerSheet,
                minPurchaseSheets,
                paymentDays,
                deliveryDays,
                priceDate,
                preferred,
                state,
                landedCostPerSheet,
                createdAt,
                updatedAt
        );
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static int requirePositivePackageUnit(Integer packageUnit) {
        if (packageUnit == null || packageUnit <= 0) {
            throw new IllegalArgumentException("La unidad de empaque debe ser mayor que 0");
        }
        return packageUnit;
    }

    private static BigDecimal positiveMoney(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nonNegativeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El flete no puede ser negativo");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static Integer positiveOptionalInt(Integer value, String message) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static Integer nonNegativeOptionalInt(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException("Los días no pueden ser negativos");
        }
        return value;
    }

    public String getPaperSupplierPriceId() {
        return paperSupplierPriceId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getPaperId() {
        return paperId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public BigDecimal getSheetValue() {
        return sheetValue;
    }

    public int getPackageUnit() {
        return packageUnit;
    }

    public BigDecimal getFreightPerSheet() {
        return freightPerSheet;
    }

    public Integer getMinPurchaseSheets() {
        return minPurchaseSheets;
    }

    public Integer getPaymentDays() {
        return paymentDays;
    }

    public Integer getDeliveryDays() {
        return deliveryDays;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public boolean isState() {
        return state;
    }

    public BigDecimal getLandedCostPerSheet() {
        return landedCostPerSheet;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaperSupplierPrice that)) {
            return false;
        }
        return Objects.equals(paperSupplierPriceId, that.paperSupplierPriceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paperSupplierPriceId);
    }
}
