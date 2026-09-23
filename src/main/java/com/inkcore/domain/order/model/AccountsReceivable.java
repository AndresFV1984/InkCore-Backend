package com.inkcore.domain.order.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AccountsReceivable {

    private String accountsReceivableId;
    private String companyId;
    private String cxcNumber;
    private String abonosNumber;
    private String productionOrderId;
    private String clientId;
    private int totalUnits;
    private int deliveredUnits;
    private int pendingUnits;
    private BigDecimal totalOwed;
    private BigDecimal totalPaid;
    private BigDecimal totalRemaining;
    private BigDecimal totalCashPaid;
    private BigDecimal totalWithheld;
    private BigDecimal totalAdvancePaid;
    private LocalDateTime openedAt;
    private LocalDate dueDate;
    private int paymentTermDays;
    private AccountsReceivableStatus status;
    private LocalDateTime lastDeliveryAt;
    private String lastPaymentNumber;
    private LocalDateTime lastPaymentAt;
    private LocalDateTime updatedAt;

    public AccountsReceivable() {
        this.totalOwed = BigDecimal.ZERO;
        this.totalPaid = BigDecimal.ZERO;
        this.totalRemaining = BigDecimal.ZERO;
        this.totalCashPaid = BigDecimal.ZERO;
        this.totalWithheld = BigDecimal.ZERO;
        this.totalAdvancePaid = BigDecimal.ZERO;
        this.paymentTermDays = 0;
        this.status = AccountsReceivableStatus.PENDIENTE;
    }

    public String getAccountsReceivableId() { return accountsReceivableId; }
    public void setAccountsReceivableId(String accountsReceivableId) { this.accountsReceivableId = accountsReceivableId; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getCxcNumber() { return cxcNumber; }
    public void setCxcNumber(String cxcNumber) { this.cxcNumber = cxcNumber; }
    public String getAbonosNumber() { return abonosNumber; }
    public void setAbonosNumber(String abonosNumber) { this.abonosNumber = abonosNumber; }
    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public int getTotalUnits() { return totalUnits; }
    public void setTotalUnits(int totalUnits) { this.totalUnits = totalUnits; }
    public int getDeliveredUnits() { return deliveredUnits; }
    public void setDeliveredUnits(int deliveredUnits) { this.deliveredUnits = deliveredUnits; }
    public int getPendingUnits() { return pendingUnits; }
    public void setPendingUnits(int pendingUnits) { this.pendingUnits = pendingUnits; }
    public BigDecimal getTotalOwed() { return totalOwed; }
    public void setTotalOwed(BigDecimal totalOwed) { this.totalOwed = totalOwed; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
    public BigDecimal getTotalRemaining() { return totalRemaining; }
    public void setTotalRemaining(BigDecimal totalRemaining) { this.totalRemaining = totalRemaining; }
    public BigDecimal getTotalCashPaid() { return totalCashPaid; }
    public void setTotalCashPaid(BigDecimal totalCashPaid) { this.totalCashPaid = totalCashPaid; }
    public BigDecimal getTotalWithheld() { return totalWithheld; }
    public void setTotalWithheld(BigDecimal totalWithheld) { this.totalWithheld = totalWithheld; }
    public BigDecimal getTotalAdvancePaid() { return totalAdvancePaid; }
    public void setTotalAdvancePaid(BigDecimal totalAdvancePaid) { this.totalAdvancePaid = totalAdvancePaid; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public int getPaymentTermDays() { return paymentTermDays; }
    public void setPaymentTermDays(int paymentTermDays) { this.paymentTermDays = paymentTermDays; }
    public AccountsReceivableStatus getStatus() { return status; }
    public void setStatus(AccountsReceivableStatus status) { this.status = status; }
    public LocalDateTime getLastDeliveryAt() { return lastDeliveryAt; }
    public void setLastDeliveryAt(LocalDateTime lastDeliveryAt) { this.lastDeliveryAt = lastDeliveryAt; }
    public String getLastPaymentNumber() { return lastPaymentNumber; }
    public void setLastPaymentNumber(String lastPaymentNumber) { this.lastPaymentNumber = lastPaymentNumber; }
    public LocalDateTime getLastPaymentAt() { return lastPaymentAt; }
    public void setLastPaymentAt(LocalDateTime lastPaymentAt) { this.lastPaymentAt = lastPaymentAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
