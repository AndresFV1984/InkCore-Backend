package com.inkcore.domain.papertype.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Asignación de un despiece a un tipo de papel (valor de corte opcional).
 */
public final class PaperTypeCutAssignment {

    private final String cutLayoutId;
    private final BigDecimal cutValue;

    private PaperTypeCutAssignment(String cutLayoutId, BigDecimal cutValue) {
        this.cutLayoutId = cutLayoutId;
        this.cutValue = cutValue;
    }

    public static PaperTypeCutAssignment of(String cutLayoutId, BigDecimal cutValue) {
        if (cutLayoutId == null || cutLayoutId.isBlank()) {
            throw new IllegalArgumentException("El despiece es obligatorio");
        }
        BigDecimal normalized = null;
        if (cutValue != null) {
            if (cutValue.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El valor de corte no puede ser negativo");
            }
            normalized = cutValue.setScale(2, RoundingMode.HALF_UP);
        }
        return new PaperTypeCutAssignment(cutLayoutId.trim(), normalized);
    }

    public String getCutLayoutId() {
        return cutLayoutId;
    }

    public BigDecimal getCutValue() {
        return cutValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaperTypeCutAssignment that)) return false;
        return Objects.equals(cutLayoutId, that.cutLayoutId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cutLayoutId);
    }
}
