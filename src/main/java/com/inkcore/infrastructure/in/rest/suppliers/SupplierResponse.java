package com.inkcore.infrastructure.in.rest.suppliers;

import com.inkcore.domain.supplier.model.Supplier;
import com.inkcore.infrastructure.in.rest.users.DepartmentResponse;
import com.inkcore.infrastructure.in.rest.users.DocumentTypeResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "SupplierResponse", description = "Proveedor registrado. documentType anidado como en clientes/usuarios.")
public record SupplierResponse(
        @Schema(description = "Identificador único del proveedor", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String supplierId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre o razón social", example = "Papeles del Norte S.A.S.")
        String name,

        @Schema(description = "Documento (tipo y número)")
        DocumentTypeResponse documentType,

        @Schema(description = "Ubicación (departamento y ciudad)")
        DepartmentResponse department,

        @Schema(description = "Dirección", example = "Carrera 50 # 25-10")
        String address,

        @Schema(description = "Teléfono", example = "604 555 1234")
        String phone,

        @Schema(description = "Correo electrónico", example = "ventas@papelesdelnorte.com")
        String email,

        @Schema(description = "Persona de contacto", example = "María López")
        String contactPerson,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Fecha de registro", example = "2026-08-27")
        LocalDate creationDate
) {
    public static SupplierResponse from(Supplier s) {
        return new SupplierResponse(
                s.getSupplierId(),
                s.getCompanyId(),
                s.getName(),
                DocumentTypeResponse.of(s.getDocumentType(), s.getIdentification()),
                DepartmentResponse.of(s.getDepartment(), s.getCity()),
                s.getAddress(),
                s.getPhone(),
                s.getEmail(),
                s.getContactPerson(),
                s.isState(),
                s.getCreationDate()
        );
    }
}
