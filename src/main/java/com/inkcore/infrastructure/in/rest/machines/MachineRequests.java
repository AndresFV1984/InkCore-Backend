package com.inkcore.infrastructure.in.rest.machines;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class MachineRequests {

    private MachineRequests() {
    }

    @Schema(
            name = "CreateMachineRequest",
            description = "Alta de máquina. companyId sale del JWT. costPerHour lo calcula la base y no se envía. "
                    + "Solo un preferred=true por machineType. Si preferred se omite y no hay preferida del tipo, "
                    + "esta queda como preferida. setupMinutes se precarga en la OP al crear."
    )
    public record CreateMachineRequest(
            @NotBlank @Size(max = 150)
            @Schema(description = "Nombre único por compañía", example = "Offset Heidelberg 4 colores",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String name,
            @NotBlank
            @Schema(
                    description = "Fase de la orden a la que pertenece",
                    allowableValues = {"preprensa", "corte-papel", "impresion", "terminados", "acabados"},
                    example = "impresion",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String machineType,
            @Size(max = 150)
            @Schema(description = "Fabricante", example = "Heidelberg")
            String manufacturer,
            @Size(max = 150)
            @Schema(description = "Modelo", example = "SM 74")
            String model,
            @NotNull @PositiveOrZero
            @Schema(description = "Costo de adquisición", example = "250000000.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal purchaseCost,
            @NotNull @Positive
            @Schema(description = "Vida útil en años", example = "10.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal usefulLifeYears,
            @PositiveOrZero
            @Schema(description = "Mantenimiento anual. 0 si se omite.", example = "8000000.00")
            BigDecimal annualMaintenanceCost,
            @PositiveOrZero
            @Schema(description = "Costo mensual del operario. 0 si se omite.", example = "2500000.00")
            BigDecimal monthlyOperatorCost,
            @PositiveOrZero
            @Schema(description = "Energía por hora. 0 si se omite.", example = "15000.00")
            BigDecimal energyCostPerHour,
            @NotNull @Positive
            @Schema(description = "Horas productivas reales al año", example = "1600.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal productiveHoursPerYear,
            @PositiveOrZero
            @Schema(description = "Minutos de arranque/calibración por defecto al cotizar en OP. 0 si se omite.",
                    example = "15")
            Integer setupMinutes,
            @Schema(description = "true=preferida de la fase (solo una por machineType). "
                    + "Si se omite y no hay preferida del tipo, queda true.",
                    example = "true")
            Boolean preferred,
            @Schema(description = "true=activa. Default true. Inactiva no puede ser preferida.", example = "true")
            Boolean state
    ) {
    }

    @Schema(
            name = "UpdateMachineRequest",
            description = "Edición de máquina, incluida la desactivación con state=false. costPerHour no se envía. "
                    + "Solo un preferred=true por machineType. state=false fuerza preferred=false."
    )
    public record UpdateMachineRequest(
            @NotBlank @Size(max = 150)
            @Schema(description = "Nombre único por compañía", example = "Offset Heidelberg 4 colores",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String name,
            @NotBlank
            @Schema(
                    allowableValues = {"preprensa", "corte-papel", "impresion", "terminados", "acabados"},
                    example = "impresion",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String machineType,
            @Size(max = 150)
            @Schema(example = "Heidelberg")
            String manufacturer,
            @Size(max = 150)
            @Schema(example = "SM 74")
            String model,
            @NotNull @PositiveOrZero
            @Schema(example = "250000000.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal purchaseCost,
            @NotNull @Positive
            @Schema(example = "10.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal usefulLifeYears,
            @PositiveOrZero
            @Schema(example = "8500000.00")
            BigDecimal annualMaintenanceCost,
            @PositiveOrZero
            @Schema(example = "2500000.00")
            BigDecimal monthlyOperatorCost,
            @PositiveOrZero
            @Schema(example = "15000.00")
            BigDecimal energyCostPerHour,
            @NotNull @Positive
            @Schema(example = "1600.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal productiveHoursPerYear,
            @PositiveOrZero
            @Schema(description = "Minutos de arranque/calibración por defecto. Si se omite, conserva el actual.",
                    example = "15")
            Integer setupMinutes,
            @Schema(description = "true=preferida de la fase. Si se omite, conserva el valor actual "
                    + "(salvo cambio de machineType o state=false).",
                    example = "true")
            Boolean preferred,
            @NotNull
            @Schema(description = "false desactiva la máquina (y quita preferred)", example = "true",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Boolean state
    ) {
    }
}
