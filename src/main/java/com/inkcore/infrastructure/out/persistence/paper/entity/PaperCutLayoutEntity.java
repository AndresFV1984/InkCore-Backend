package com.inkcore.infrastructure.out.persistence.paper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paper_cut_layouts", schema = "indicolors")
public class PaperCutLayoutEntity implements Persistable<String> {

    @Id
    @Column(name = "paper_cut_layout_id", length = 64)
    private String paperCutLayoutId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "paper_id", nullable = false, length = 64)
    private String paperId;

    @Column(name = "cut_layout_id", nullable = false, length = 64)
    private String cutLayoutId;

    @Column(length = 16)
    private String orientation;

    @Column(name = "waste_percentage", precision = 5, scale = 2)
    private BigDecimal wastePercentage;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private boolean state;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PaperCutLayoutEntity() {
    }

    @Override
    public String getId() {
        return paperCutLayoutId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    public String getPaperCutLayoutId() {
        return paperCutLayoutId;
    }

    public void setPaperCutLayoutId(String paperCutLayoutId) {
        this.paperCutLayoutId = paperCutLayoutId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(String paperId) {
        this.paperId = paperId;
    }

    public String getCutLayoutId() {
        return cutLayoutId;
    }

    public void setCutLayoutId(String cutLayoutId) {
        this.cutLayoutId = cutLayoutId;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public BigDecimal getWastePercentage() {
        return wastePercentage;
    }

    public void setWastePercentage(BigDecimal wastePercentage) {
        this.wastePercentage = wastePercentage;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
