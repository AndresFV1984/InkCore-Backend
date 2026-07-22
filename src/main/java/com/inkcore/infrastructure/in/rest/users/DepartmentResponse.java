package com.inkcore.infrastructure.in.rest.users;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DepartmentInfo", description = "Departamento y ciudad")
public record DepartmentResponse(
        @Schema(description = "Departamento", example = "Antioquia")
        String department,
        @Schema(description = "Ciudad / municipio", example = "Medellín")
        String city
) {
    public static DepartmentResponse of(String department, String city) {
        return new DepartmentResponse(
                department == null ? "" : department,
                city == null ? "" : city
        );
    }
}
