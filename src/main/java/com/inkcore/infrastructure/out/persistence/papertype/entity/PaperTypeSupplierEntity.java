package com.inkcore.infrastructure.out.persistence.papertype.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "paper_type_suppliers", schema = "indicolors")
public class PaperTypeSupplierEntity {

    @EmbeddedId
    private PaperTypeSupplierId id;

    @Column(name = "sheet_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal sheetValue;

    @Column(name = "package_unit", nullable = false)
    private Integer packageUnit;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    public PaperTypeSupplierEntity() {
    }

    public PaperTypeSupplierId getId() {
        return id;
    }

    public void setId(PaperTypeSupplierId id) {
        this.id = id;
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

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    @Embeddable
    public static class PaperTypeSupplierId implements Serializable {

        @Column(name = "paper_type_id", length = 64)
        private String paperTypeId;

        @Column(name = "supplier_id", length = 64)
        private String supplierId;

        public PaperTypeSupplierId() {
        }

        public PaperTypeSupplierId(String paperTypeId, String supplierId) {
            this.paperTypeId = paperTypeId;
            this.supplierId = supplierId;
        }

        public String getPaperTypeId() {
            return paperTypeId;
        }

        public void setPaperTypeId(String paperTypeId) {
            this.paperTypeId = paperTypeId;
        }

        public String getSupplierId() {
            return supplierId;
        }

        public void setSupplierId(String supplierId) {
            this.supplierId = supplierId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PaperTypeSupplierId that)) return false;
            return Objects.equals(paperTypeId, that.paperTypeId)
                    && Objects.equals(supplierId, that.supplierId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paperTypeId, supplierId);
        }
    }
}
