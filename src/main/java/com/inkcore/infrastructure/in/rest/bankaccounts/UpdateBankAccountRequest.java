package com.inkcore.infrastructure.in.rest.bankaccounts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(
        name = "UpdateBankAccountRequest",
        description = """
                Actualización de cuenta bancaria (PUT /api/v1/bank-accounts/update/{accountId}).
                Obligatorios: bankName, accountType, accountNumber, holderName, includeInPdf, isPrimary, state.
                Opcional: holderNit. No envía companyId (se conserva el existente).
                """
)
public record UpdateBankAccountRequest(
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

        @Schema(description = "Uso en documentos: true=Incluir en PDF", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean includeInPdf,

        @Schema(description = "Cuenta principal de la compañía (solo una)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean isPrimary,

        @Schema(description = "Estado: true=Activa, false=Inactiva", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean state
) {
}
