package com.inkcore.infrastructure.in.rest.sellers;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(
        name = "CreateSellerRequest",
        description = """
                Alta de vendedor (POST /api/v1/sellers/register).
                Obligatorios: companyId, fullName, documentType, identification, email, department, city.
                Opcionales: phone, address, state (default true).
                """
)
public record CreateSellerRequest(
        @Schema(description = "Identificador de empresa", example = "company-seed-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La empresa es obligatoria")
        @Size(max = 64)
        String companyId,

        @Schema(description = "Nombre completo", example = "Carlos Andrés Gómez", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 200)
        String fullName,

        @Schema(
                description = "Tipo de documento",
                example = "CC",
                allowableValues = {"CC", "CE", "TI", "PA", "NIT"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Size(max = 20)
        String documentType,

        @Schema(description = "Número de identificación", example = "1020304050", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El número de identificación es obligatorio")
        @Size(max = 32)
        String identification,

        @Schema(description = "Correo electrónico", example = "vendedor@empresa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "Correo electrónico inválido")
        @Size(max = 320)
        String email,

        @Schema(description = "Teléfono / contacto", example = "300 123 4567")
        @Size(max = 32)
        String phone,

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

        @Schema(description = "Estado: true=activo, false=inactivo (default true)", example = "true")
        Boolean state
) {
}
