package com.inkcore.domain.papertype.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Agregado tipo de papel. Campos alineados a {@code indicolors.paper_types}
 * y asignaciones N:M en {@code paper_type_cut_layouts}.
 */
public final class PaperType {

    private static final Set<String> ALLOWED_UNITS = Set.of("cm", "mm", "in");

    private final String paperTypeId;
    private final String companyId;
    private final String name;
    private final BigDecimal width;
    private final BigDecimal height;
    private final String unit;
    private final BigDecimal sheetValue;
    private final int packageUnit;
    private final boolean coated;
    private final boolean state;
    private final LocalDate creationDate;
    private final List<PaperTypeCutAssignment> cutAssignments;

    private PaperType(
            String paperTypeId,
            String companyId,
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal sheetValue,
            int packageUnit,
            boolean coated,
            boolean state,
            LocalDate creationDate,
            List<PaperTypeCutAssignment> cutAssignments
    ) {
        this.paperTypeId = paperTypeId;
        this.companyId = companyId;
        this.name = name;
        this.width = width;
        this.height = height;
        this.unit = unit;
        this.sheetValue = sheetValue;
        this.packageUnit = packageUnit;
        this.coated = coated;
        this.state = state;
        this.creationDate = creationDate;
        this.cutAssignments = List.copyOf(cutAssignments);
    }

    public static PaperType createNew(
            String companyId,
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal sheetValue,
            int packageUnit,
            boolean coated,
            boolean state,
            LocalDate creationDate,
            List<PaperTypeCutAssignment> cutAssignments
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(name, "El nombre es obligatorio");
        return new PaperType(
                UUID.randomUUID().toString(),
                companyId.trim(),
                name.trim(),
                normalizePositiveDimension(width, "El ancho debe ser mayor que 0"),
                normalizePositiveDimension(height, "El alto debe ser mayor que 0"),
                normalizeUnit(unit),
                normalizeNonNegativeMoney(sheetValue, "El valor de la hoja no puede ser negativo"),
                requirePositivePackageUnit(packageUnit),
                coated,
                state,
                creationDate,
                dedupeAssignments(cutAssignments)
        );
    }

    public PaperType update(
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal sheetValue,
            int packageUnit,
            boolean coated,
            boolean state,
            List<PaperTypeCutAssignment> cutAssignments
    ) {
        requireNotBlank(name, "El nombre es obligatorio");
        return new PaperType(
                this.paperTypeId,
                this.companyId,
                name.trim(),
                normalizePositiveDimension(width, "El ancho debe ser mayor que 0"),
                normalizePositiveDimension(height, "El alto debe ser mayor que 0"),
                normalizeUnit(unit),
                normalizeNonNegativeMoney(sheetValue, "El valor de la hoja no puede ser negativo"),
                requirePositivePackageUnit(packageUnit),
                coated,
                state,
                this.creationDate,
                dedupeAssignments(cutAssignments)
        );
    }

    public static PaperType reconstitute(
            String paperTypeId,
            String companyId,
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal sheetValue,
            int packageUnit,
            boolean coated,
            boolean state,
            LocalDate creationDate,
            List<PaperTypeCutAssignment> cutAssignments
    ) {
        return new PaperType(
                paperTypeId,
                companyId,
                name,
                width,
                height,
                unit,
                sheetValue,
                packageUnit,
                coated,
                state,
                creationDate,
                cutAssignments == null ? List.of() : cutAssignments
        );
    }

    private static List<PaperTypeCutAssignment> dedupeAssignments(List<PaperTypeCutAssignment> cutAssignments) {
        if (cutAssignments == null || cutAssignments.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, PaperTypeCutAssignment> byId = new LinkedHashMap<>();
        for (PaperTypeCutAssignment assignment : cutAssignments) {
            if (assignment != null) {
                byId.put(assignment.getCutLayoutId(), assignment);
            }
        }
        return new ArrayList<>(byId.values());
    }

    private static BigDecimal normalizePositiveDimension(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeNonNegativeMoney(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalArgumentException("El valor de la hoja es obligatorio");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
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

    private static int requirePositivePackageUnit(int packageUnit) {
        if (packageUnit <= 0) {
            throw new IllegalArgumentException("La unidad de empaque debe ser mayor que 0");
        }
        return packageUnit;
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public String getPaperTypeId() {
        return paperTypeId;
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

    public BigDecimal getSheetValue() {
        return sheetValue;
    }

    public int getPackageUnit() {
        return packageUnit;
    }

    public boolean isCoated() {
        return coated;
    }

    public boolean isState() {
        return state;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public List<PaperTypeCutAssignment> getCutAssignments() {
        return cutAssignments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaperType that)) return false;
        return Objects.equals(paperTypeId, that.paperTypeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paperTypeId);
    }
}
