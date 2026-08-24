package com.inkcore.domain.productionorder.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de Terminados/Acabados por plancha
 * ({@code indicolors.production_order_postpress_records}).
 */
public final class PostpressRecord {

    private String recordId;
    private String companyId;
    private String productionOrderId;
    private String plateId;

    private PostpressType type;
    private boolean completed;

    private List<PostpressLine> lines = new ArrayList<>();

    public PostpressRecord() {
        this.recordId = UUID.randomUUID().toString();
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(String productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public String getPlateId() {
        return plateId;
    }

    public void setPlateId(String plateId) {
        this.plateId = plateId;
    }

    public PostpressType getType() {
        return type;
    }

    public void setType(PostpressType type) {
        this.type = type;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public List<PostpressLine> getLines() {
        return lines;
    }

    public void setLines(List<PostpressLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PostpressRecord that)) {
            return false;
        }
        return Objects.equals(recordId, that.recordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }
}
