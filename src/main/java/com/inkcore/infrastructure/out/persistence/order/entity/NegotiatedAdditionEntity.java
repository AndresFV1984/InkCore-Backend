package com.inkcore.infrastructure.out.persistence.order.entity;

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
@Table(name = "order_negotiated_additions", schema = "indicolors")
public class NegotiatedAdditionEntity implements Persistable<String> {

    @Id
    @Column(name = "order_negotiated_addition_id", length = 64)
    private String orderNegotiatedAdditionId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "addition_number", nullable = false, length = 32)
    private String additionNumber;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "movement_type", nullable = false, length = 16)
    private String movementType;

    @Column(nullable = false, length = 200)
    private String concept;

    @Column(name = "change_reason", nullable = false, length = 32)
    private String changeReason;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "schedule_impact_days", nullable = false)
    private int scheduleImpactDays;

    @Column(name = "approved_by_client_name", nullable = false, length = 150)
    private String approvedByClientName;

    @Column(name = "approval_channel", nullable = false, length = 32)
    private String approvalChannel;

    @Column(name = "approval_reference", length = 120)
    private String approvalReference;

    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt;

    @Column(name = "reversed_addition_id", length = 64)
    private String reversedAdditionId;

    @Column(name = "registered_by", nullable = false, length = 64)
    private String registeredBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "voided_by", length = 64)
    private String voidedBy;

    @Override
    public String getId() { return orderNegotiatedAdditionId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }

    public void markNew() { this.isNew = true; }

    public String getOrderNegotiatedAdditionId() { return orderNegotiatedAdditionId; }
    public void setOrderNegotiatedAdditionId(String orderNegotiatedAdditionId) {
        this.orderNegotiatedAdditionId = orderNegotiatedAdditionId;
    }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getAdditionNumber() { return additionNumber; }
    public void setAdditionNumber(String additionNumber) { this.additionNumber = additionNumber; }
    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public int getScheduleImpactDays() { return scheduleImpactDays; }
    public void setScheduleImpactDays(int scheduleImpactDays) { this.scheduleImpactDays = scheduleImpactDays; }
    public String getApprovedByClientName() { return approvedByClientName; }
    public void setApprovedByClientName(String approvedByClientName) {
        this.approvedByClientName = approvedByClientName;
    }
    public String getApprovalChannel() { return approvalChannel; }
    public void setApprovalChannel(String approvalChannel) { this.approvalChannel = approvalChannel; }
    public String getApprovalReference() { return approvalReference; }
    public void setApprovalReference(String approvalReference) { this.approvalReference = approvalReference; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getReversedAdditionId() { return reversedAdditionId; }
    public void setReversedAdditionId(String reversedAdditionId) { this.reversedAdditionId = reversedAdditionId; }
    public String getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getVoidedAt() { return voidedAt; }
    public void setVoidedAt(LocalDateTime voidedAt) { this.voidedAt = voidedAt; }
    public String getVoidedBy() { return voidedBy; }
    public void setVoidedBy(String voidedBy) { this.voidedBy = voidedBy; }
}
