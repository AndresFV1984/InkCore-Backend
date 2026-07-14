package com.indicore.infrastructure.in.rest.users;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(
        name = "UpdateUserRequest",
        description = "Datos para actualizar un usuario. La contraseña es opcional: si se omite o va vacía, no se cambia."
)
public record UpdateUserRequest(
        @Schema(description = "Nombre completo", example = "María Fernanda López", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200)
        String name,

        @Schema(
                description = "Tipo de documento",
                example = "CC",
                allowableValues = {"CC", "CE", "TI", "PA", "NIT"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank @Size(max = 20)
        String documentType,

        @Schema(description = "Número de identificación", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 64)
        String identificationNumber,

        @Schema(description = "Correo electrónico", example = "usuario@empresa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email @Size(max = 320)
        String mail,

        @Schema(description = "Teléfono / contacto (opcional)", example = "300 123 4567")
        @Size(max = 300)
        String contact,

        @Schema(description = "Departamento", example = "Antioquia", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100)
        String department,

        @Schema(description = "Ciudad / municipio", example = "Medellín", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100)
        String city,

        @Schema(description = "Dirección (opcional)", example = "Calle 10 # 20-30, El Poblado")
        @Size(max = 255)
        String address,

        @Schema(
                description = "Código del rol (usar GET /api/v1/roles/list)",
                example = "OPERADOR",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank @Size(max = 50)
        String roleCode,

        @ArraySchema(
                arraySchema = @Schema(description = "Códigos de permisos seleccionados (usar GET /api/v1/permissions/list)"),
                schema = @Schema(example = "GESTIONAR_MI_TRABAJO_PRODUCCION")
        )
        List<@NotBlank @Size(max = 80) String> permissionCodes,

        @Schema(
                description = "Nueva contraseña (opcional). Si se envía, mínimo 6 caracteres.",
                example = "nuevaClave1",
                maxLength = 100
        )
        @Size(max = 100)
        String password,

        @Schema(
                description = "Estado del usuario: true = Activo, false = Inactivo",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        Boolean state
) {
}
