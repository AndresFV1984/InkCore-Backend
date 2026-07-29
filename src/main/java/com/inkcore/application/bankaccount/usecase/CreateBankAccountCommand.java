package com.inkcore.application.bankaccount.usecase;

public record CreateBankAccountCommand(
        String companyId,
        String bankName,
        String accountType,
        String accountNumber,
        String holderName,
        String holderNit,
        Boolean includeInPdf,
        Boolean primary,
        Boolean state
) {
}
