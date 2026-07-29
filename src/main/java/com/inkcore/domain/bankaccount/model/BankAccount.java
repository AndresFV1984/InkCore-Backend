package com.inkcore.domain.bankaccount.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Agregado cuenta bancaria. Campos alineados a {@code indicolors.bank_accounts}.
 */
public final class BankAccount {

    private static final Set<String> ALLOWED_TYPES = Set.of("Ahorros", "Corriente");

    private final String accountId;
    private final String companyId;
    private final String bankName;
    private final String accountType;
    private final String accountNumber;
    private final String holderName;
    private final String holderNit;
    private final boolean includeInPdf;
    private final boolean primary;
    private final boolean state;
    private final LocalDate creationDate;

    private BankAccount(
            String accountId,
            String companyId,
            String bankName,
            String accountType,
            String accountNumber,
            String holderName,
            String holderNit,
            boolean includeInPdf,
            boolean primary,
            boolean state,
            LocalDate creationDate
    ) {
        this.accountId = accountId;
        this.companyId = companyId;
        this.bankName = bankName;
        this.accountType = accountType;
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.holderNit = holderNit != null ? holderNit : "";
        this.includeInPdf = includeInPdf;
        this.primary = primary;
        this.state = state;
        this.creationDate = creationDate;
    }

    public static BankAccount createNew(
            String companyId,
            String bankName,
            String accountType,
            String accountNumber,
            String holderName,
            String holderNit,
            boolean includeInPdf,
            boolean primary,
            boolean state,
            LocalDate creationDate
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(bankName, "El banco es obligatorio");
        requireNotBlank(accountType, "El tipo de cuenta es obligatorio");
        requireNotBlank(accountNumber, "El número de cuenta es obligatorio");
        requireNotBlank(holderName, "El nombre / razón social del titular es obligatorio");

        return new BankAccount(
                UUID.randomUUID().toString(),
                companyId.trim(),
                bankName.trim(),
                normalizeAccountType(accountType),
                accountNumber.trim(),
                holderName.trim(),
                blankToEmpty(holderNit),
                includeInPdf,
                primary,
                state,
                creationDate
        );
    }

    public BankAccount update(
            String bankName,
            String accountType,
            String accountNumber,
            String holderName,
            String holderNit,
            boolean includeInPdf,
            boolean primary,
            boolean state
    ) {
        requireNotBlank(bankName, "El banco es obligatorio");
        requireNotBlank(accountType, "El tipo de cuenta es obligatorio");
        requireNotBlank(accountNumber, "El número de cuenta es obligatorio");
        requireNotBlank(holderName, "El nombre / razón social del titular es obligatorio");

        return new BankAccount(
                this.accountId,
                this.companyId,
                bankName.trim(),
                normalizeAccountType(accountType),
                accountNumber.trim(),
                holderName.trim(),
                blankToEmpty(holderNit),
                includeInPdf,
                primary,
                state,
                this.creationDate
        );
    }

    public static BankAccount reconstitute(
            String accountId,
            String companyId,
            String bankName,
            String accountType,
            String accountNumber,
            String holderName,
            String holderNit,
            boolean includeInPdf,
            boolean primary,
            boolean state,
            LocalDate creationDate
    ) {
        return new BankAccount(
                accountId,
                companyId,
                bankName,
                accountType,
                accountNumber,
                holderName,
                holderNit,
                includeInPdf,
                primary,
                state,
                creationDate
        );
    }

    private static String normalizeAccountType(String accountType) {
        String trimmed = accountType.trim();
        for (String allowed : ALLOWED_TYPES) {
            if (allowed.equalsIgnoreCase(trimmed)) {
                return allowed;
            }
        }
        throw new IllegalArgumentException("Tipo de cuenta inválido. Use Ahorros o Corriente");
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getHolderNit() {
        return holderNit;
    }

    public boolean isIncludeInPdf() {
        return includeInPdf;
    }

    public boolean isPrimary() {
        return primary;
    }

    public boolean isState() {
        return state;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankAccount that)) return false;
        return Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }
}
