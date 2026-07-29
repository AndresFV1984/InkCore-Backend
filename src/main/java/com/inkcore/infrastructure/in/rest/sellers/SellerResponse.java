package com.inkcore.infrastructure.in.rest.sellers;

import com.inkcore.domain.seller.model.Seller;
import com.inkcore.infrastructure.in.rest.users.DepartmentResponse;
import com.inkcore.infrastructure.in.rest.users.DocumentTypeResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "SellerResponse", description = "Vendedor registrado. documentType anidado como en clientes/usuarios.")
public record SellerResponse(
        @Schema(description = "Identificador único del vendedor", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String sellerId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre completo", example = "Carlos Andrés Gómez")
        String fullName,

        @Schema(description = "Documento (tipo y número)")
        DocumentTypeResponse documentType,

        @Schema(description = "Correo electrónico", example = "vendedor@empresa.com")
        String email,

        @Schema(description = "Teléfono / contacto", example = "300 123 4567")
        String phone,

        @Schema(description = "Ubicación (departamento y ciudad)")
        DepartmentResponse department,

        @Schema(description = "Dirección", example = "Calle 10 # 20-30")
        String address,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Fecha de registro", example = "2026-07-25")
        LocalDate creationDate
) {
    public static SellerResponse from(Seller s) {
        return new SellerResponse(
                s.getSellerId(),
                s.getCompanyId(),
                s.getFullName(),
                DocumentTypeResponse.of(s.getDocumentType(), s.getIdentification()),
                s.getEmail(),
                s.getPhone(),
                DepartmentResponse.of(s.getDepartment(), s.getCity()),
                s.getAddress(),
                s.isState(),
                s.getCreationDate()
        );
    }
}
