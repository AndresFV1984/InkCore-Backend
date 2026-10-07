package com.inkcore.domain.paper.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Remanente reutilizable de corte: material del papel origen con medidas propias.
 */
public final class PaperRemnant {

    private final String paperRemnantId;
    private final String companyId;
    private final String paperId;
    private final BigDecimal width;
    private final BigDecimal height;
    private final String unit;
    private final BigDecimal quantityInitial;
    private final BigDecimal quantityAvailable;
    private final BigDecimal unitCost;
    private final String sourceProductionOrderId;
    private final String sourcePaperRowId;
    private final LocalDate entryDate;
    private final String note;
    private final boolean state;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private PaperRemnant(
            String paperRemnantId,
            String companyId,
            String paperId,
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            String sourceProductionOrderId,
            String sourcePaperRowId,
            LocalDate entryDate,
            String note,
            boolean state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.paperRemnantId = paperRemnantId;
        this.companyId = companyId;
        this.paperId = paperId;
        this.width = width;
        this.height = height;
        this.unit = unit;
        this.quantityInitial = quantityInitial;
        this.quantityAvailable = quantityAvailable;
        this.unitCost = unitCost;
        this.sourceProductionOrderId = sourceProductionOrderId;
        this.sourcePaperRowId = sourcePaperRowId;
        this.entryDate = entryDate;
        this.note = note;
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaperRemnant createNew(
            String companyId,
            String paperId,
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            String sourceProductionOrderId,
            String sourcePaperRowId,
            LocalDate entryDate,
            String note,
            Boolean state,
            LocalDateTime now
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(paperId, "El papel es obligatorio");
        Objects.requireNonNull(entryDate, "La fecha de ingreso es obligatoria");
        BigDecimal initial = nonNegativeQty(quantityInitial, "La cantidad inicial no puede ser negativa");
        BigDecimal available = quantityAvailable == null
                ? initial
                : nonNegativeQty(quantityAvailable, "La cantidad disponible no puede ser negativa");
        if (available.compareTo(initial) > 0) {
            throw new IllegalArgumentException("La cantidad disponible no puede superar la inicial");
        }
        return new PaperRemnant(
                UUID.randomUUID().toString(),
                companyId.trim(),
                paperId.trim(),
                Paper.normalizePositiveDimension(width, "El ancho debe ser mayor que 0"),
                Paper.normalizePositiveDimension(height, "El alto debe ser mayor que 0"),
                Paper.normalizeUnit(unit),
                initial,
                available,
                nonNegativeMoney(unitCost),
                blankToNull(sourceProductionOrderId),
                blankToNull(sourcePaperRowId),
                entryDate,
                blankToNull(note),
                state == null || state,
                now,
                now
        );
    }

    /**
     * Descuenta unidades disponibles (consumo desde corte de papel).
     */
    public PaperRemnant consume(BigDecimal quantity, LocalDateTime updatedAt) {
        BigDecimal qty = nonNegativeQty(quantity, "La cantidad a descontar no puede ser negativa");
        if (qty.compareTo(BigDecimal.ZERO) == 0) {
            return this;
        }
        if (quantityAvailable.compareTo(qty) < 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente del remanente: disponible "
                            + quantityAvailable.toPlainString()
                            + ", solicitado "
                            + qty.toPlainString()
            );
        }
        return new PaperRemnant(
                paperRemnantId,
                companyId,
                paperId,
                width,
                height,
                unit,
                quantityInitial,
                quantityAvailable.subtract(qty).setScale(2, RoundingMode.HALF_UP),
                unitCost,
                sourceProductionOrderId,
                sourcePaperRowId,
                entryDate,
                note,
                state,
                createdAt,
                Objects.requireNonNull(updatedAt, "updatedAt es obligatorio")
        );
    }

    /**
     * Devuelve unidades al stock (p. ej. al anular la OP o cambiar el origen del corte).
     * No supera {@code quantityInitial}.
     */
    public PaperRemnant restore(BigDecimal quantity, LocalDateTime updatedAt) {
        BigDecimal qty = nonNegativeQty(quantity, "La cantidad a devolver no puede ser negativa");
        if (qty.compareTo(BigDecimal.ZERO) == 0) {
            return this;
        }
        BigDecimal next = quantityAvailable.add(qty);
        if (next.compareTo(quantityInitial) > 0) {
            next = quantityInitial;
        }
        return new PaperRemnant(
                paperRemnantId,
                companyId,
                paperId,
                width,
                height,
                unit,
                quantityInitial,
                next.setScale(2, RoundingMode.HALF_UP),
                unitCost,
                sourceProductionOrderId,
                sourcePaperRowId,
                entryDate,
                note,
                state,
                createdAt,
                Objects.requireNonNull(updatedAt, "updatedAt es obligatorio")
        );
    }

    public PaperRemnant update(
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            String sourceProductionOrderId,
            String sourcePaperRowId,
            LocalDate entryDate,
            String note,
            Boolean state,
            LocalDateTime updatedAt
    ) {
        BigDecimal initial = nonNegativeQty(quantityInitial, "La cantidad inicial no puede ser negativa");
        BigDecimal available = nonNegativeQty(quantityAvailable, "La cantidad disponible no puede ser negativa");
        if (available.compareTo(initial) > 0) {
            throw new IllegalArgumentException("La cantidad disponible no puede superar la inicial");
        }
        return new PaperRemnant(
                paperRemnantId,
                companyId,
                paperId,
                Paper.normalizePositiveDimension(width, "El ancho debe ser mayor que 0"),
                Paper.normalizePositiveDimension(height, "El alto debe ser mayor que 0"),
                Paper.normalizeUnit(unit),
                initial,
                available,
                nonNegativeMoney(unitCost),
                blankToNull(sourceProductionOrderId),
                blankToNull(sourcePaperRowId),
                Objects.requireNonNullElse(entryDate, this.entryDate),
                blankToNull(note),
                state == null ? this.state : state,
                createdAt,
                updatedAt
        );
    }

    public static PaperRemnant reconstitute(
            String paperRemnantId,
            String companyId,
            String paperId,
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            String sourceProductionOrderId,
            String sourcePaperRowId,
            LocalDate entryDate,
            String note,
            boolean state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new PaperRemnant(
                paperRemnantId, companyId, paperId, width, height, unit,
                quantityInitial, quantityAvailable, unitCost,
                sourceProductionOrderId, sourcePaperRowId, entryDate, note, state,
                createdAt, updatedAt
        );
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static BigDecimal nonNegativeQty(BigDecimal value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nonNegativeMoney(BigDecimal value) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        if (resolved.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El costo unitario no puede ser negativo");
        }
        return resolved.setScale(2, RoundingMode.HALF_UP);
    }

    public String getPaperRemnantId() {
        return paperRemnantId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getPaperId() {
        return paperId;
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

    public BigDecimal getQuantityInitial() {
        return quantityInitial;
    }

    public BigDecimal getQuantityAvailable() {
        return quantityAvailable;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public String getSourceProductionOrderId() {
        return sourceProductionOrderId;
    }

    public String getSourcePaperRowId() {
        return sourcePaperRowId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public String getNote() {
        return note;
    }

    public boolean isState() {
        return state;
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
        if (!(o instanceof PaperRemnant that)) {
            return false;
        }
        return Objects.equals(paperRemnantId, that.paperRemnantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paperRemnantId);
    }
}
