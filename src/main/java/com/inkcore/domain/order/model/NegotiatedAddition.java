package com.inkcore.domain.order.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class NegotiatedAddition {

    private String orderNegotiatedAdditionId;
    private String companyId;
    private String additionNumber;
    private String productionOrderId;
    private String clientId;
    private NegotiatedAdditionMovementType movementType;
    private String concept;
    private NegotiatedAdditionReason changeReason;
    private BigDecimal amount;
    private int scheduleImpactDays;
    private String approvedByClientName;
    private NegotiatedAdditionApprovalChannel approvalChannel;
    private String approvalReference;
    private LocalDateTime approvedAt;
    private String reversedAdditionId;
    private String registeredBy;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime voidedAt;
    private String voidedBy;

    public NegotiatedAddition() {
        this.orderNegotiatedAdditionId = UUID.randomUUID().toString();
        this.movementType = NegotiatedAdditionMovementType.REGISTRO;
        this.amount = BigDecimal.ZERO;
    }

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
    public NegotiatedAdditionMovementType getMovementType() { return movementType; }
    public void setMovementType(NegotiatedAdditionMovementType movementType) { this.movementType = movementType; }
    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }
    public NegotiatedAdditionReason getChangeReason() { return changeReason; }
    public void setChangeReason(NegotiatedAdditionReason changeReason) { this.changeReason = changeReason; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public int getScheduleImpactDays() { return scheduleImpactDays; }
    public void setScheduleImpactDays(int scheduleImpactDays) { this.scheduleImpactDays = scheduleImpactDays; }
    public String getApprovedByClientName() { return approvedByClientName; }
    public void setApprovedByClientName(String approvedByClientName) {
        this.approvedByClientName = approvedByClientName;
    }
    public NegotiatedAdditionApprovalChannel getApprovalChannel() { return approvalChannel; }
    public void setApprovalChannel(NegotiatedAdditionApprovalChannel approvalChannel) {
        this.approvalChannel = approvalChannel;
    }
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

    public boolean isVoided() {
        return voidedAt != null;
    }
}
