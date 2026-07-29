package com.inkcore.infrastructure.in.rest.bankaccounts;

import com.inkcore.domain.bankaccount.model.BankAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "BankAccountResponse", description = "Cuenta bancaria registrada (formulario Nueva cuenta bancaria)")
public record BankAccountResponse(
        @Schema(description = "Identificador único de la cuenta", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String accountId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre del banco", example = "Bancolombia")
        String bankName,

        @Schema(description = "Tipo de cuenta: Ahorros o Corriente", example = "Corriente")
        String accountType,

        @Schema(description = "Número de cuenta", example = "12345678901")
        String accountNumber,

        @Schema(description = "Nombre / razón social del titular", example = "InkCore S.A.S.")
        String holderName,

        @Schema(description = "NIT del titular", example = "900.000.000-1")
        String holderNit,

        @Schema(description = "true = Incluir en PDF de costeo", example = "true")
        boolean includeInPdf,

        @Schema(description = "true = Cuenta principal de la compañía", example = "false")
        boolean isPrimary,

        @Schema(description = "true = Activa, false = Inactiva", example = "true")
        boolean state,

        @Schema(description = "Fecha de registro", example = "2026-07-28")
        LocalDate creationDate
) {
    public static BankAccountResponse from(BankAccount a) {
        return new BankAccountResponse(
                a.getAccountId(),
                a.getCompanyId(),
                a.getBankName(),
                a.getAccountType(),
                a.getAccountNumber(),
                a.getHolderName(),
                a.getHolderNit(),
                a.isIncludeInPdf(),
                a.isPrimary(),
                a.isState(),
                a.getCreationDate()
        );
    }
}
