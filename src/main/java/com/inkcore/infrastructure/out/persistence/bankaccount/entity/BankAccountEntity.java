package com.inkcore.infrastructure.out.persistence.bankaccount.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;

@Entity
@Table(name = "bank_accounts", schema = "indicolors")
public class BankAccountEntity implements Persistable<String> {

    @Id
    @Column(name = "account_id", length = 64)
    private String accountId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "account_type", nullable = false, length = 30)
    private String accountType;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "holder_name", nullable = false, length = 200)
    private String holderName;

    @Column(name = "holder_nit", length = 32)
    private String holderNit;

    @Column(name = "include_in_pdf", nullable = false)
    private boolean includeInPdf;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(nullable = false)
    private boolean state;

    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    public BankAccountEntity() {
    }

    @Override
    public String getId() {
        return accountId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    public String getHolderNit() {
        return holderNit;
    }

    public void setHolderNit(String holderNit) {
        this.holderNit = holderNit;
    }

    public boolean isIncludeInPdf() {
        return includeInPdf;
    }

    public void setIncludeInPdf(boolean includeInPdf) {
        this.includeInPdf = includeInPdf;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }
}
