package com.inkcore.domain.paper.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Lote de inventario propio de papel.
 */
public final class PaperStock {

    private final String paperStockId;
    private final String companyId;
    private final String paperId;
    private final BigDecimal quantityInitial;
    private final BigDecimal quantityAvailable;
    private final BigDecimal unitCost;
    private final LocalDate entryDate;
    private final boolean state;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private PaperStock(
            String paperStockId,
            String companyId,
            String paperId,
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            LocalDate entryDate,
            boolean state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.paperStockId = paperStockId;
        this.companyId = companyId;
        this.paperId = paperId;
        this.quantityInitial = quantityInitial;
        this.quantityAvailable = quantityAvailable;
        this.unitCost = unitCost;
        this.entryDate = entryDate;
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaperStock createNew(
            String companyId,
            String paperId,
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            LocalDate entryDate,
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
        return new PaperStock(
                UUID.randomUUID().toString(),
                companyId.trim(),
                paperId.trim(),
                initial,
                available,
                nonNegativeMoney(unitCost),
                entryDate,
                state == null || state,
                now,
                now
        );
    }

    public PaperStock update(
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            LocalDate entryDate,
            Boolean state,
            LocalDateTime updatedAt
    ) {
        BigDecimal initial = nonNegativeQty(quantityInitial, "La cantidad inicial no puede ser negativa");
        BigDecimal available = nonNegativeQty(quantityAvailable, "La cantidad disponible no puede ser negativa");
        if (available.compareTo(initial) > 0) {
            throw new IllegalArgumentException("La cantidad disponible no puede superar la inicial");
        }
        return new PaperStock(
                paperStockId,
                companyId,
                paperId,
                initial,
                available,
                nonNegativeMoney(unitCost),
                Objects.requireNonNullElse(entryDate, this.entryDate),
                state == null ? this.state : state,
                createdAt,
                updatedAt
        );
    }

    public static PaperStock reconstitute(
            String paperStockId,
            String companyId,
            String paperId,
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            LocalDate entryDate,
            boolean state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new PaperStock(
                paperStockId,
                companyId,
                paperId,
                quantityInitial,
                quantityAvailable,
                unitCost,
                entryDate,
                state,
                createdAt,
                updatedAt
        );
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
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
        if (value == null) {
            throw new IllegalArgumentException("El costo unitario es obligatorio");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El costo unitario no puede ser negativo");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public String getPaperStockId() {
        return paperStockId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getPaperId() {
        return paperId;
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

    public LocalDate getEntryDate() {
        return entryDate;
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
        if (!(o instanceof PaperStock that)) {
            return false;
        }
        return Objects.equals(paperStockId, that.paperStockId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paperStockId);
    }
}
