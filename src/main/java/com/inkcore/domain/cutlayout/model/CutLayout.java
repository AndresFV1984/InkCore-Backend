package com.inkcore.domain.cutlayout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Agregado despiece (catálogo). Campos alineados a {@code indicolors.cut_layouts}.
 */
public final class CutLayout {

    private static final Set<String> ALLOWED_UNITS = Set.of("cm", "mm", "in");

    private final String cutLayoutId;
    private final String companyId;
    private final String name;
    private final BigDecimal width;
    private final BigDecimal height;
    private final String unit;
    private final int piecesPerSheet;
    private final boolean state;
    private final LocalDate creationDate;

    private CutLayout(
            String cutLayoutId,
            String companyId,
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            int piecesPerSheet,
            boolean state,
            LocalDate creationDate
    ) {
        this.cutLayoutId = cutLayoutId;
        this.companyId = companyId;
        this.name = name;
        this.width = width;
        this.height = height;
        this.unit = unit;
        this.piecesPerSheet = piecesPerSheet;
        this.state = state;
        this.creationDate = creationDate;
    }

    public static CutLayout createNew(
            String companyId,
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            int piecesPerSheet,
            boolean state,
            LocalDate creationDate
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(name, "El nombre es obligatorio");

        return new CutLayout(
                UUID.randomUUID().toString(),
                companyId.trim(),
                name.trim(),
                normalizePositiveDimension(width, "El ancho debe ser mayor que 0"),
                normalizePositiveDimension(height, "El alto debe ser mayor que 0"),
                normalizeUnit(unit),
                requirePositivePieces(piecesPerSheet),
                state,
                creationDate
        );
    }

    public CutLayout update(
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            int piecesPerSheet,
            boolean state
    ) {
        requireNotBlank(name, "El nombre es obligatorio");
        return new CutLayout(
                this.cutLayoutId,
                this.companyId,
                name.trim(),
                normalizePositiveDimension(width, "El ancho debe ser mayor que 0"),
                normalizePositiveDimension(height, "El alto debe ser mayor que 0"),
                normalizeUnit(unit),
                requirePositivePieces(piecesPerSheet),
                state,
                this.creationDate
        );
    }

    public static CutLayout reconstitute(
            String cutLayoutId,
            String companyId,
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            int piecesPerSheet,
            boolean state,
            LocalDate creationDate
    ) {
        return new CutLayout(
                cutLayoutId,
                companyId,
                name,
                width,
                height,
                unit,
                piecesPerSheet,
                state,
                creationDate
        );
    }

    private static BigDecimal normalizePositiveDimension(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalizeUnit(String unit) {
        String normalized = unit == null || unit.isBlank() ? "cm" : unit.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_UNITS.contains(normalized)) {
            throw new IllegalArgumentException("La unidad debe ser cm, mm o in");
        }
        return normalized;
    }

    private static int requirePositivePieces(int piecesPerSheet) {
        if (piecesPerSheet <= 0) {
            throw new IllegalArgumentException("Las piezas por pliego deben ser mayor que 0");
        }
        return piecesPerSheet;
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public String getCutLayoutId() {
        return cutLayoutId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public String getUnit() {
        return unit;
    }

    public int getPiecesPerSheet() {
        return piecesPerSheet;
    }

    public boolean isState() {
        return state;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CutLayout that)) return false;
        return Objects.equals(cutLayoutId, that.cutLayoutId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cutLayoutId);
    }
}
