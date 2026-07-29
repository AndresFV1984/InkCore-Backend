package com.inkcore.infrastructure.in.rest.bankaccounts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
        name = "CreateBankAccountRequest",
        description = """
                Alta de cuenta bancaria (POST /api/v1/bank-accounts/register).
                Obligatorios: companyId, bankName, accountType (Ahorros|Corriente), accountNumber, holderName.
                Opcionales: holderNit, includeInPdf (default true), isPrimary (default false), state (default true).
                """
)
public record CreateBankAccountRequest(
        @Schema(description = "Identificador de empresa", example = "company-seed-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La empresa es obligatoria")
        @Size(max = 64)
        String companyId,

        @Schema(description = "Nombre del banco", example = "Bancolombia", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El banco es obligatorio")
        @Size(max = 150)
        String bankName,

        @Schema(
                description = "Tipo de cuenta",
                example = "Corriente",
                allowableValues = {"Ahorros", "Corriente"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "El tipo de cuenta es obligatorio")
        @Size(max = 30)
        String accountType,

        @Schema(description = "Número de cuenta", example = "12345678901", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El número de cuenta es obligatorio")
        @Size(max = 50)
        String accountNumber,

        @Schema(description = "Nombre / razón social del titular", example = "InkCore S.A.S.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre / razón social del titular es obligatorio")
        @Size(max = 200)
        String holderName,

        @Schema(description = "NIT del titular (opcional)", example = "900.000.000-1")
        @Size(max = 32)
        String holderNit,

        @Schema(description = "Uso en documentos: true=Incluir en PDF (default true)", example = "true")
        Boolean includeInPdf,

        @Schema(description = "Cuenta principal de la compañía (solo una; default false)", example = "false")
        Boolean isPrimary,

        @Schema(description = "Estado: true=Activa, false=Inactiva (default true)", example = "true")
        Boolean state
) {
}
