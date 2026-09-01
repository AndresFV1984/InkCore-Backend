package com.inkcore.infrastructure.in.rest.suppliers;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
        name = "CreateSupplierRequest",
        description = """
                Alta de proveedor (POST /api/v1/suppliers/register).
                Obligatorios: companyId, name, department, city.
                Opcionales: documentType (CC|CE|TI|PA|NIT), identification, address, phone, email, contactPerson, state (default true).
                """
)
public record CreateSupplierRequest(
        @Schema(description = "Identificador de empresa", example = "company-seed-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La empresa es obligatoria")
        @Size(max = 64)
        String companyId,

        @Schema(description = "Nombre o razón social", example = "Papeles del Norte S.A.S.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre o razón social es obligatorio")
        @Size(max = 200)
        String name,

        @Schema(
                description = "Tipo de documento",
                example = "NIT",
                allowableValues = {"CC", "CE", "TI", "PA", "NIT"}
        )
        @Size(max = 20)
        String documentType,

        @Schema(description = "NIT / C.C.", example = "900987654-3")
        @Size(max = 32)
        String identification,

        @Schema(description = "Departamento", example = "Antioquia", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El departamento es obligatorio")
        @Size(max = 100)
        String department,

        @Schema(description = "Ciudad / municipio", example = "Medellín", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La ciudad / municipio es obligatorio")
        @Size(max = 120)
        String city,

        @Schema(description = "Dirección (calle, barrio, referencia)", example = "Carrera 50 # 25-10")
        @Size(max = 255)
        String address,

        @Schema(description = "Teléfono de contacto", example = "604 555 1234")
        @Size(max = 32)
        String phone,

        @Schema(description = "Correo electrónico", example = "ventas@papelesdelnorte.com")
        @Email(message = "Correo electrónico inválido")
        @Size(max = 320)
        String email,

        @Schema(description = "Persona de contacto principal", example = "María López")
        @Size(max = 200)
        String contactPerson,

        @Schema(description = "Estado: true=activo, false=inactivo (default true)", example = "true")
        Boolean state
) {
}
