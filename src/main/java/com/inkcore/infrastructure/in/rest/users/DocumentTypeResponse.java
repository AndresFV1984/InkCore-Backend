package com.inkcore.infrastructure.in.rest.users;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DocumentTypeInfo", description = "Tipo y número de documento")
public record DocumentTypeResponse(
        @Schema(description = "Tipo de documento", example = "CC")
        String documentType,
        @Schema(description = "Número de identificación", example = "1234567890")
        String identificationNumber
) {
    public static DocumentTypeResponse of(String documentType, String identificationNumber) {
        return new DocumentTypeResponse(
                documentType == null ? "" : documentType,
                identificationNumber == null ? "" : identificationNumber
        );
    }
}
