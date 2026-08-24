package com.inkcore.infrastructure.out.persistence.productionorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Raíz JPA de production_orders.
 * <p>
 * No implementa {@code Persistable}: con ID asignado + {@code @Version}, Spring Data
 * trata {@code version == null} como entidad nueva (persist) y versión no nula como
 * existente (merge). Forzar {@code Persistable.isNew()=true} con versión ya asignada
 * provoca {@code detached entity passed to persist}.
 */
@Entity
@Table(name = "production_orders", schema = "indicolors")
public class ProductionOrderEntity {

    @Id
    @Column(name = "production_order_id", length = 64)
    private String productionOrderId;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "work_name", nullable = false, length = 150)
    private String workName;

    @Column(name = "seller_id", length = 64)
    private String sellerId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "requested_quantity", nullable = false)
    private int requestedQuantity;

    @Column(name = "proposal_quantity_1")
    private Integer proposalQuantity1;

    @Column(name = "proposal_quantity_2")
    private Integer proposalQuantity2;

    @Column(name = "specifications_completed_at")
    private LocalDateTime specificationsCompletedAt;

    @Column(name = "cutting_completed_at")
    private LocalDateTime cuttingCompletedAt;

    @Column(name = "printing_completed_at")
    private LocalDateTime printingCompletedAt;

    @Column(name = "finished_products_completed_at")
    private LocalDateTime finishedProductsCompletedAt;

    @Column(name = "finishing_processes_completed_at")
    private LocalDateTime finishingProcessesCompletedAt;

    @Column(name = "client_supplies_paper_default")
    private Boolean clientSuppliesPaperDefault;

    @Column(name = "rounding_margin")
    private Integer roundingMargin;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "state", nullable = false)
    private boolean state;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    public ProductionOrderEntity() {
    }

    public String getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(String productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getWorkName() {
        return workName;
    }

    public void setWorkName(String workName) {
        this.workName = workName;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Integer getProposalQuantity1() {
        return proposalQuantity1;
    }

    public void setProposalQuantity1(Integer proposalQuantity1) {
        this.proposalQuantity1 = proposalQuantity1;
    }

    public Integer getProposalQuantity2() {
        return proposalQuantity2;
    }

    public void setProposalQuantity2(Integer proposalQuantity2) {
        this.proposalQuantity2 = proposalQuantity2;
    }

    public LocalDateTime getSpecificationsCompletedAt() {
        return specificationsCompletedAt;
    }

    public void setSpecificationsCompletedAt(LocalDateTime specificationsCompletedAt) {
        this.specificationsCompletedAt = specificationsCompletedAt;
    }

    public LocalDateTime getCuttingCompletedAt() {
        return cuttingCompletedAt;
    }

    public void setCuttingCompletedAt(LocalDateTime cuttingCompletedAt) {
        this.cuttingCompletedAt = cuttingCompletedAt;
    }

    public LocalDateTime getPrintingCompletedAt() {
        return printingCompletedAt;
    }

    public void setPrintingCompletedAt(LocalDateTime printingCompletedAt) {
        this.printingCompletedAt = printingCompletedAt;
    }

    public LocalDateTime getFinishedProductsCompletedAt() {
        return finishedProductsCompletedAt;
    }

    public void setFinishedProductsCompletedAt(LocalDateTime finishedProductsCompletedAt) {
        this.finishedProductsCompletedAt = finishedProductsCompletedAt;
    }

    public LocalDateTime getFinishingProcessesCompletedAt() {
        return finishingProcessesCompletedAt;
    }

    public void setFinishingProcessesCompletedAt(LocalDateTime finishingProcessesCompletedAt) {
        this.finishingProcessesCompletedAt = finishingProcessesCompletedAt;
    }

    public Boolean getClientSuppliesPaperDefault() {
        return clientSuppliesPaperDefault;
    }

    public void setClientSuppliesPaperDefault(Boolean clientSuppliesPaperDefault) {
        this.clientSuppliesPaperDefault = clientSuppliesPaperDefault;
    }

    public Integer getRoundingMargin() {
        return roundingMargin;
    }

    public void setRoundingMargin(Integer roundingMargin) {
        this.roundingMargin = roundingMargin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
