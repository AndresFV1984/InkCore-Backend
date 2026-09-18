package com.inkcore.infrastructure.out.persistence.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts_receivable", schema = "indicolors")
public class AccountsReceivableEntity {

    @Id
    @Column(name = "accounts_receivable_id", length = 64)
    private String accountsReceivableId;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "cxc_number", nullable = false, length = 32)
    private String cxcNumber;

    @Column(name = "production_order_id", nullable = false, length = 64, unique = true)
    private String productionOrderId;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "total_units", nullable = false)
    private int totalUnits;

    @Column(name = "delivered_units", nullable = false)
    private int deliveredUnits;

    @Column(name = "pending_units", nullable = false)
    private int pendingUnits;

    @Column(name = "total_owed", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalOwed;

    @Column(name = "total_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPaid;

    @Column(name = "total_remaining", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRemaining;

    @Column(name = "total_cash_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCashPaid;

    @Column(name = "total_withheld", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalWithheld;

    @Column(name = "total_advance_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAdvancePaid;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_term_days", nullable = false)
    private int paymentTermDays;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "last_delivery_at")
    private LocalDateTime lastDeliveryAt;

    @Column(name = "last_payment_number", length = 32)
    private String lastPaymentNumber;

    @Column(name = "last_payment_at")
    private LocalDateTime lastPaymentAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getAccountsReceivableId() { return accountsReceivableId; }
    public void setAccountsReceivableId(String accountsReceivableId) { this.accountsReceivableId = accountsReceivableId; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getCxcNumber() { return cxcNumber; }
    public void setCxcNumber(String cxcNumber) { this.cxcNumber = cxcNumber; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastDeliveryAt() { return lastDeliveryAt; }
    public void setLastDeliveryAt(LocalDateTime lastDeliveryAt) { this.lastDeliveryAt = lastDeliveryAt; }
    public String getLastPaymentNumber() { return lastPaymentNumber; }
    public void setLastPaymentNumber(String lastPaymentNumber) { this.lastPaymentNumber = lastPaymentNumber; }
    public LocalDateTime getLastPaymentAt() { return lastPaymentAt; }
    public void setLastPaymentAt(LocalDateTime lastPaymentAt) { this.lastPaymentAt = lastPaymentAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
