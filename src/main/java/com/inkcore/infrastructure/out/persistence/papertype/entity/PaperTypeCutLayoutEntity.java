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
@Table(name = "paper_type_cut_layouts", schema = "indicolors")
public class PaperTypeCutLayoutEntity {

    @EmbeddedId
    private PaperTypeCutLayoutId id;

    @Column(name = "cut_value", precision = 12, scale = 2)
    private BigDecimal cutValue;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    public PaperTypeCutLayoutEntity() {
    }

    public PaperTypeCutLayoutId getId() {
        return id;
    }

    public void setId(PaperTypeCutLayoutId id) {
        this.id = id;
    }

    public BigDecimal getCutValue() {
        return cutValue;
    }

    public void setCutValue(BigDecimal cutValue) {
        this.cutValue = cutValue;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    @Embeddable
    public static class PaperTypeCutLayoutId implements Serializable {

        @Column(name = "paper_type_id", length = 64)
        private String paperTypeId;

        @Column(name = "cut_layout_id", length = 64)
        private String cutLayoutId;

        public PaperTypeCutLayoutId() {
        }

        public PaperTypeCutLayoutId(String paperTypeId, String cutLayoutId) {
            this.paperTypeId = paperTypeId;
            this.cutLayoutId = cutLayoutId;
        }

        public String getPaperTypeId() {
            return paperTypeId;
        }

        public void setPaperTypeId(String paperTypeId) {
            this.paperTypeId = paperTypeId;
        }

        public String getCutLayoutId() {
            return cutLayoutId;
        }

        public void setCutLayoutId(String cutLayoutId) {
            this.cutLayoutId = cutLayoutId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PaperTypeCutLayoutId that)) return false;
            return Objects.equals(paperTypeId, that.paperTypeId)
                    && Objects.equals(cutLayoutId, that.cutLayoutId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paperTypeId, cutLayoutId);
        }
    }
}
