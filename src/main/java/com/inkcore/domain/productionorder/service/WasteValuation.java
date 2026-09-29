package com.inkcore.domain.productionorder.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Cálculo de merma planificada y reparto del desperdicio real.
 * El costo persistido ({@code planned_cost}/{@code actual_cost}/
 * {@code estimated_machine_cost}) lo materializa la columna generada de la BD;
 * aquí se arman las cantidades y el costo unitario que el servidor envía.
 */
public final class WasteValuation {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 2;

    private WasteValuation() {
    }

    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentage(BigDecimal requested, BigDecimal companyDefault) {
        BigDecimal value = requested == null ? companyDefault : requested;
        if (value == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("El porcentaje de merma debe estar entre 0 y 100");
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal plannedQuantity(Integer baseUnits, BigDecimal percentage) {
        return plannedQuantity(baseUnits, percentage, BigDecimal.ZERO);
    }

    /**
     * Pliegos fijos de arranque más el porcentaje sobre la base.
     * Con fijos en 0 el resultado coincide con el cálculo solo por porcentaje.
     */
    public static BigDecimal plannedQuantity(Integer baseUnits, BigDecimal percentage, BigDecimal fixedQuantity) {
        BigDecimal fixed = money(fixedQuantity);
        if (fixed.signum() < 0) {
            throw new IllegalArgumentException("Los pliegos fijos de arranque no pueden ser negativos");
        }
        BigDecimal variable = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        if (baseUnits != null && baseUnits > 0 && percentage != null && percentage.signum() > 0) {
            variable = BigDecimal.valueOf(baseUnits.longValue())
                    .multiply(percentage)
                    .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
        }
        return fixed.add(variable).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Reparte la cantidad observada sobre las mermas planificadas, en orden.
     * Lo que supera la suma planificada es desperdicio (no planificado).
     */
    public static Allocation allocate(List<BigDecimal> plannedQuantities, BigDecimal observed) {
        BigDecimal remaining = money(observed).max(BigDecimal.ZERO);
        List<BigDecimal> assigned = new ArrayList<>();
        if (plannedQuantities != null) {
            for (BigDecimal planned : plannedQuantities) {
                BigDecimal cap = money(planned).max(BigDecimal.ZERO);
                BigDecimal take = remaining.min(cap);
                assigned.add(take);
                remaining = remaining.subtract(take);
            }
        }
        return new Allocation(List.copyOf(assigned), money(remaining));
    }

    /**
     * Costo unitario del desperdicio: material + máquina de la fase prorrateada
     * + tinta prorrateada, sobre la base producida (pliegos o unidades).
     */
    public static BigDecimal proratedUnitCost(
            BigDecimal materialUnitCost,
            BigDecimal phaseMachineCost,
            BigDecimal phaseInkCost,
            BigDecimal baseQuantity
    ) {
        BigDecimal base = baseQuantity == null || baseQuantity.signum() <= 0
                ? BigDecimal.ONE
                : baseQuantity;
        BigDecimal machinePerUnit = money(phaseMachineCost).divide(base, 8, RoundingMode.HALF_UP);
        BigDecimal inkPerUnit = money(phaseInkCost).divide(base, 8, RoundingMode.HALF_UP);
        return money(materialUnitCost).add(machinePerUnit).add(inkPerUnit).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public record Allocation(List<BigDecimal> assignedToPlanned, BigDecimal excess) {
    }
}
