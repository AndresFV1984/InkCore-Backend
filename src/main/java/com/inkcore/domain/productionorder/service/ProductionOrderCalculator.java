package com.inkcore.domain.productionorder.service;

import com.inkcore.domain.productionorder.model.DiscountType;
import com.inkcore.domain.productionorder.model.FlipType;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PrintConfig;
import com.inkcore.domain.productionorder.model.PrintEntry;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.thousandrate.model.ThousandRate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cálculos de servidor de la Orden de Producción. Todos los importes se
 * devuelven con escala 2 y redondeo HALF_UP.
 */
public final class ProductionOrderCalculator {

    private static final int MONEY_SCALE = 2;
    private static final int THOUSANDS_SCALE = 2;
    private static final int RAW_SCALE = 6;
    private static final Pattern FIRST_INTEGER = Pattern.compile("\\d+");

    private ProductionOrderCalculator() {
    }

    /**
     * Pliegos buenos = cantidad / cavidades (división entera). Sin cavidades no
     * hay dato calculable.
     */
    public static Integer calculateGoodSizes(Integer quantity, Integer cavities) {
        if (quantity == null || cavities == null || cavities == 0) {
            return null;
        }
        return quantity / cavities;
    }

    public static BigDecimal calculatePlateTotalValue(BigDecimal platePrice, Integer platesCount) {
        BigDecimal price = platePrice == null ? BigDecimal.ZERO : platePrice;
        int count = platesCount == null ? 1 : platesCount;
        return money(price.multiply(BigDecimal.valueOf(count)));
    }

    public static BigDecimal calculateTotalPlatesValue(List<Plate> plates) {
        if (plates == null || plates.isEmpty()) {
            return money(BigDecimal.ZERO);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Plate plate : plates) {
            if (plate.getTotalValue() != null) {
                total = total.add(plate.getTotalValue());
            }
        }
        return money(total);
    }

    /**
     * Pliegos necesarios = techo((pliegos buenos + margen) / piezas por pliego).
     */
    public static Integer calculateSheets(Integer goodSizes, Integer piecesPerSheet, Integer roundingMargin) {
        if (goodSizes == null || piecesPerSheet == null || piecesPerSheet == 0) {
            return null;
        }
        int margin = roundingMargin == null ? 0 : roundingMargin;
        BigDecimal numerator = BigDecimal.valueOf((long) goodSizes + margin);
        return numerator
                .divide(BigDecimal.valueOf(piecesPerSheet), 0, RoundingMode.CEILING)
                .intValue();
    }

    public static BigDecimal calculateLineTotal(Integer units, BigDecimal unitValue) {
        if (units == null || unitValue == null) {
            return money(BigDecimal.ZERO);
        }
        return money(unitValue.multiply(BigDecimal.valueOf(units)));
    }

    /**
     * Millares facturables según las reglas de la tarifa: por debajo del tope
     * mínimo se cobra {@code minThousand}; por encima se redondea hacia arriba
     * solo si la parte decimal alcanza {@code decimalThreshold}.
     */
    public static BigDecimal calculateThousands(Integer units, ThousandRate rate) {
        if (rate == null || units == null || units <= 0) {
            return BigDecimal.ZERO.setScale(THOUSANDS_SCALE, RoundingMode.HALF_UP);
        }
        if (units < rate.getMinThresholdUnits()) {
            return rate.getMinThousand().setScale(THOUSANDS_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal raw = BigDecimal.valueOf(units)
                .divide(BigDecimal.valueOf(rate.getThousandUnit()), RAW_SCALE, RoundingMode.HALF_UP);
        BigDecimal floor = raw.setScale(0, RoundingMode.FLOOR);
        BigDecimal fraction = raw.subtract(floor);
        BigDecimal threshold = rate.getDecimalThreshold() == null
                ? BigDecimal.ZERO
                : rate.getDecimalThreshold();
        BigDecimal thousands = fraction.compareTo(threshold) >= 0 && fraction.signum() > 0
                ? floor.add(BigDecimal.ONE)
                : floor;
        return thousands.setScale(THOUSANDS_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Precio unitario del millar según el volteo elegido; si el volteo no tiene
     * precio configurado se cae al precio base de la tarifa.
     */
    public static BigDecimal resolveFlipPrice(
            FlipType flipType,
            BigDecimal basePrice,
            BigDecimal gripperFlipPrice,
            BigDecimal squareFlipPrice
    ) {
        BigDecimal fallback = basePrice == null ? BigDecimal.ZERO : basePrice;
        if (flipType == null || flipType == FlipType.NO_FLIP) {
            return fallback;
        }
        BigDecimal selected = flipType == FlipType.GRIPPER_FLIP ? gripperFlipPrice : squareFlipPrice;
        return selected == null ? fallback : selected;
    }

    public static BigDecimal calculatePrintingPrice(BigDecimal thousands, BigDecimal flipPrice) {
        if (thousands == null || flipPrice == null) {
            return money(BigDecimal.ZERO);
        }
        return money(thousands.multiply(flipPrice));
    }

    /**
     * Precio de una línea de Terminados/Acabados antes del costo mínimo.
     */
    public static BigDecimal calculatePostpressPrice(
            BigDecimal valuePerCm2,
            BigDecimal areaFactor,
            Integer goodSizes
    ) {
        if (valuePerCm2 == null || areaFactor == null || goodSizes == null) {
            return money(BigDecimal.ZERO);
        }
        return money(valuePerCm2.multiply(areaFactor).multiply(BigDecimal.valueOf(goodSizes)));
    }

    public static boolean appliesMinCost(BigDecimal calculatedPrice, BigDecimal minCost) {
        return minCost != null
                && calculatedPrice != null
                && calculatedPrice.compareTo(minCost) < 0;
    }

    /**
     * Extrae el número de tintas de etiquetas como {@code "4"} o
     * {@code "4 COLORES"}. Devuelve null cuando no es interpretable.
     */
    public static Integer parseColorCount(String colors) {
        if (colors == null || colors.isBlank()) {
            return null;
        }
        Matcher matcher = FIRST_INTEGER.matcher(colors);
        if (!matcher.find()) {
            return null;
        }
        return Integer.valueOf(matcher.group());
    }

    /**
     * Tiro + retiro debe igualar los colores de la plancha. Si los colores no
     * son numéricos no se puede validar y se acepta la entrada.
     */
    public static boolean inkCountsMatchColors(int shotsInkCount, int reverseInkCount, String colors) {
        Integer expected = parseColorCount(colors);
        if (expected == null) {
            return true;
        }
        return shotsInkCount + reverseInkCount == expected;
    }

    public static BigDecimal money(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * Aplica un descuento porcentual o de monto fijo, sin dejar el total negativo.
     */
    public static BigDecimal applyDiscount(BigDecimal base, String discountType, BigDecimal discountValue) {
        BigDecimal amount = nullSafe(base);
        if (discountType == null || discountValue == null || discountValue.signum() <= 0) {
            return money(amount);
        }
        BigDecimal discount = "%".equals(discountType)
                ? amount.multiply(discountValue).divide(BigDecimal.valueOf(100), RAW_SCALE, RoundingMode.HALF_UP)
                : discountValue;
        BigDecimal result = amount.subtract(discount);
        return money(result.signum() < 0 ? BigDecimal.ZERO : result);
    }

    /**
     * Total a cobrar de la OP (panel Cobro): suma de etapas menos descuentos de
     * preprensa/impresión/cobro. Mismo criterio que el PDF de cobro.
     * {@code null} si aún no hay datos de costo cargados.
     */
    public static BigDecimal calculateTotalToCharge(ProductionOrder order) {
        if (order == null) {
            return null;
        }
        boolean hasCostData = order.getPrepress() != null
                || (order.getPaperRows() != null && !order.getPaperRows().isEmpty())
                || (order.getPrints() != null && !order.getPrints().isEmpty())
                || (order.getPostpressRecords() != null && !order.getPostpressRecords().isEmpty())
                || order.getBilling() != null;
        if (!hasCostData) {
            return null;
        }

        BigDecimal total = BigDecimal.ZERO;
        if (order.getPrepress() != null && order.getPrepress().getTotalPlatesValue() != null) {
            total = total.add(order.getPrepress().getTotalPlatesValue());
            if (order.getPrepress().getDesignCost() != null) {
                total = total.add(order.getPrepress().getDesignCost());
            }
            if (order.getPrepress().getAssemblyPriceCost() != null) {
                total = total.add(order.getPrepress().getAssemblyPriceCost());
            }
            total = applyDiscount(
                    total,
                    DiscountType.toValue(order.getPrepress().getPrepressDiscountType()),
                    order.getPrepress().getPrepressDiscountValue()
            );
        }
        if (order.getPaperRows() != null) {
            for (PaperRow row : order.getPaperRows()) {
                total = total.add(nullSafe(row.getTotalPaperValue()));
                total = total.add(nullSafe(row.getTotalCutValue()));
            }
        }
        if (order.getPrints() != null) {
            for (PrintConfig print : order.getPrints()) {
                if (print.getEntries() != null) {
                    for (PrintEntry entry : print.getEntries()) {
                        total = total.add(nullSafe(entry.getBasicPrintingPrice()));
                        total = total.add(nullSafe(entry.getPantonePrintingPrice()));
                        total = total.add(nullSafe(entry.getPantoneInkChargePrice()));
                    }
                }
                total = total.add(nullSafe(print.getSherpaTestPrice()));
                total = applyDiscount(
                        total,
                        DiscountType.toValue(print.getPrintingDiscountType()),
                        print.getPrintingDiscountValue()
                );
            }
        }
        if (order.getPostpressRecords() != null) {
            for (PostpressRecord record : order.getPostpressRecords()) {
                if (record.getLines() == null) {
                    continue;
                }
                for (PostpressLine line : record.getLines()) {
                    total = total.add(nullSafe(line.getChargedPrice()));
                }
            }
        }
        if (order.getBilling() != null) {
            total = applyDiscount(
                    total,
                    DiscountType.toValue(order.getBilling().getBillingDiscountType()),
                    order.getBilling().getBillingDiscountValue()
            );
        }
        return money(total);
    }
}
