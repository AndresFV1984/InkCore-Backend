package com.inkcore.infrastructure.in.rest.clients;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
        name = "CreateClientRequest",
        description = """
                Alta de cliente (POST /api/v1/clients/register).
                Obligatorios: companyId, name, department, city.
                Opcionales: documentType (CC|CE|TI|PA|NIT), identification, address, phone, email, contactPerson, state (default true).
                """
)
public record CreateClientRequest(
        @Schema(description = "Identificador de empresa", example = "company-seed-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La empresa es obligatoria")
        @Size(max = 64)
        String companyId,

        @Schema(description = "Nombre o razón social", example = "Comercializadora ABC S.A.S.", requiredMode = Schema.RequiredMode.REQUIRED)
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

        @Schema(description = "NIT / C.C.", example = "900123456-1")
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

        @Schema(description = "Dirección (calle, barrio, referencia)", example = "Calle 10 # 20-30")
        @Size(max = 255)
        String address,

        @Schema(description = "Teléfono de contacto", example = "604 123 4567")
        @Size(max = 32)
        String phone,

        @Schema(description = "Correo electrónico", example = "correo@empresa.com")
        @Email(message = "Correo electrónico inválido")
        @Size(max = 320)
        String email,

        @Schema(description = "Persona de contacto principal", example = "Ana Gómez")
        @Size(max = 200)
        String contactPerson,

        @Schema(description = "Estado: true=activo, false=inactivo (default true)", example = "true")
        Boolean state
) {
}
