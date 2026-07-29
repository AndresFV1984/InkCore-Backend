package com.inkcore.application.bankaccount.usecase;

public record UpdateBankAccountCommand(
        String accountId,
        String bankName,
        String accountType,
        String accountNumber,
        String holderName,
        String holderNit,
        boolean includeInPdf,
        boolean primary,
        boolean state
) {
}
