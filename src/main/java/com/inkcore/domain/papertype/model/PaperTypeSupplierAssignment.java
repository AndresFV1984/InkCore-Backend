package com.inkcore.domain.papertype.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Asignación de un proveedor a un tipo de papel con valor hoja y unidad empaque.
 */
public final class PaperTypeSupplierAssignment {

    private final String supplierId;
    private final BigDecimal sheetValue;
    private final int packageUnit;

    private PaperTypeSupplierAssignment(String supplierId, BigDecimal sheetValue, int packageUnit) {
        this.supplierId = supplierId;
        this.sheetValue = sheetValue;
        this.packageUnit = packageUnit;
    }

    public static PaperTypeSupplierAssignment of(String supplierId, BigDecimal sheetValue, int packageUnit) {
        if (supplierId == null || supplierId.isBlank()) {
            throw new IllegalArgumentException("El proveedor es obligatorio");
        }
        if (sheetValue == null) {
            throw new IllegalArgumentException("El valor de la hoja es obligatorio");
        }
        if (sheetValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El valor de la hoja no puede ser negativo");
        }
        if (packageUnit <= 0) {
            throw new IllegalArgumentException("La unidad de empaque debe ser mayor que 0");
        }
        return new PaperTypeSupplierAssignment(
                supplierId.trim(),
                sheetValue.setScale(2, RoundingMode.HALF_UP),
                packageUnit
        );
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaperTypeSupplierAssignment that)) return false;
        return Objects.equals(supplierId, that.supplierId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supplierId);
    }
}
