package com.inkcore.infrastructure.in.rest.suppliers;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(
        name = "UpdateSupplierRequest",
        description = """
                Actualización de proveedor (PUT /api/v1/suppliers/update/{supplierId}).
                Obligatorios: name, department, city, state.
                Opcionales: documentType (CC|CE|TI|PA|NIT), identification, address, phone, email, contactPerson.
                No envía companyId (se conserva el existente).
                """
)
public record UpdateSupplierRequest(
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

        @Schema(description = "Dirección", example = "Carrera 50 # 25-10")
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

        @Schema(description = "Estado: true=activo, false=inactivo", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean state
) {
}
