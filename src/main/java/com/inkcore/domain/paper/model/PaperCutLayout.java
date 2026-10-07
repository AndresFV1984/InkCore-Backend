package com.inkcore.domain.paper.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Relación papel ↔ despiece. {@code wastePercentage} es sugerencia de UI; el cálculo de OP
 * usa {@code company_waste_settings}.
 */
public final class PaperCutLayout {

    private static final Set<String> ALLOWED_ORIENTATIONS = Set.of("vertical", "horizontal");

    private final String paperCutLayoutId;
    private final String companyId;
    private final String paperId;
    private final String cutLayoutId;
    private final String orientation;
    private final BigDecimal wastePercentage;
    private final String note;
    private final boolean state;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private PaperCutLayout(
            String paperCutLayoutId,
            String companyId,
            String paperId,
            String cutLayoutId,
            String orientation,
            BigDecimal wastePercentage,
            String note,
            boolean state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.paperCutLayoutId = paperCutLayoutId;
        this.companyId = companyId;
        this.paperId = paperId;
        this.cutLayoutId = cutLayoutId;
        this.orientation = orientation;
        this.wastePercentage = wastePercentage;
        this.note = note;
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaperCutLayout createNew(
            String companyId,
            String paperId,
            String cutLayoutId,
            String orientation,
            BigDecimal wastePercentage,
            String note,
            boolean state,
            LocalDateTime now
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(paperId, "El papel es obligatorio");
        requireNotBlank(cutLayoutId, "El despiece es obligatorio");
        return new PaperCutLayout(
                UUID.randomUUID().toString(),
                companyId.trim(),
                paperId.trim(),
                cutLayoutId.trim(),
                normalizeOrientation(orientation),
                normalizeWaste(wastePercentage),
                note == null ? null : note.trim(),
                state,
                now,
                now
        );
    }

    public PaperCutLayout update(
            String orientation,
            BigDecimal wastePercentage,
            String note,
            boolean state,
            LocalDateTime updatedAt
    ) {
        return new PaperCutLayout(
                paperCutLayoutId,
                companyId,
                paperId,
                cutLayoutId,
                normalizeOrientation(orientation),
                normalizeWaste(wastePercentage),
                note == null ? null : note.trim(),
                state,
                createdAt,
                updatedAt
        );
    }

    public static PaperCutLayout reconstitute(
            String paperCutLayoutId,
            String companyId,
            String paperId,
            String cutLayoutId,
            String orientation,
            BigDecimal wastePercentage,
            String note,
            boolean state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new PaperCutLayout(
                paperCutLayoutId,
                companyId,
                paperId,
                cutLayoutId,
                orientation,
                wastePercentage,
                note,
                state,
                createdAt,
                updatedAt
        );
    }

    private static String normalizeOrientation(String orientation) {
        if (orientation == null || orientation.isBlank()) {
            return null;
        }
        String normalized = orientation.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_ORIENTATIONS.contains(normalized)) {
            throw new IllegalArgumentException("La orientación debe ser vertical u horizontal");
        }
        return normalized;
    }

    private static BigDecimal normalizeWaste(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje de desperdicio sugerido debe estar entre 0 y 100");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public String getPaperCutLayoutId() {
        return paperCutLayoutId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getPaperId() {
        return paperId;
    }

    public String getCutLayoutId() {
        return cutLayoutId;
    }

    public String getOrientation() {
        return orientation;
    }

    public BigDecimal getWastePercentage() {
        return wastePercentage;
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
        if (!(o instanceof PaperCutLayout that)) {
            return false;
        }
        return Objects.equals(paperCutLayoutId, that.paperCutLayoutId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paperCutLayoutId);
    }
}
