package com.inkcore.infrastructure.in.rest.clients;

import com.inkcore.domain.client.model.Client;
import com.inkcore.infrastructure.in.rest.users.DepartmentResponse;
import com.inkcore.infrastructure.in.rest.users.DocumentTypeResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "ClientResponse", description = "Cliente registrado. documentType anidado como en usuarios.")
public record ClientResponse(
        @Schema(description = "Identificador único del cliente", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String clientId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre o razón social", example = "Comercializadora ABC S.A.S.")
        String name,

        @Schema(description = "Documento (tipo y número)")
        DocumentTypeResponse documentType,

        @Schema(description = "Ubicación (departamento y ciudad)")
        DepartmentResponse department,

        @Schema(description = "Dirección", example = "Calle 10 # 20-30")
        String address,

        @Schema(description = "Teléfono", example = "604 123 4567")
        String phone,

        @Schema(description = "Correo electrónico", example = "correo@empresa.com")
        String email,

        @Schema(description = "Persona de contacto", example = "Ana Gómez")
        String contactPerson,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Fecha de registro", example = "2026-07-22")
        LocalDate creationDate
) {
    public static ClientResponse from(Client c) {
        return new ClientResponse(
                c.getClientId(),
                c.getCompanyId(),
                c.getName(),
                DocumentTypeResponse.of(c.getDocumentType(), c.getIdentification()),
                DepartmentResponse.of(c.getDepartment(), c.getCity()),
                c.getAddress(),
                c.getPhone(),
                c.getEmail(),
                c.getContactPerson(),
                c.isState(),
                c.getCreationDate()
        );
    }
}
