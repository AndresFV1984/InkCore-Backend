package com.inkcore.domain.paper.service;

import com.inkcore.domain.paper.model.PriceRule;
import com.inkcore.domain.productionorder.service.ProductionOrderCalculator;
import com.inkcore.domain.productionorder.service.WasteValuation;
import com.inkcore.domain.wastesettings.model.CompanyWasteSettings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Cálculo puro de costo de papel + corte. Sin acceso a BD.
 * <p>
 * La merma reutiliza {@link WasteValuation#plannedQuantity(Integer, BigDecimal, BigDecimal)}
 * (pliegos fijos de arranque + porcentaje), igual que Corte de papel.
 * El costo de máquina NO entra aquí: lo cubre {@code production_order_machine_usage}.
 * Dinero con escala 2 y {@link RoundingMode#HALF_UP}.
 */
public final class PaperCostCalculator {

    private static final int MONEY_SCALE = 2;

    private PaperCostCalculator() {
    }

    public record PriceCandidate(
            String supplierId,
            String supplierName,
            BigDecimal sheetValue,
            BigDecimal freightPerSheet,
            BigDecimal landedCostPerSheet,
            boolean ivaIncluded,
            Integer minPurchaseSheets,
            Integer deliveryDays,
            boolean preferred,
            boolean priceStale,
            Integer daysSincePriceDate
    ) {
        public BigDecimal landed() {
            return money(sheetValue).add(money(freightPerSheet));
        }
    }

    public record CostBreakdown(
            int netSheets,
            BigDecimal wasteSheets,
            BigDecimal totalSheets,
            BigDecimal sheetValueUsed,
            BigDecimal freightUsed,
            BigDecimal paperCost,
            BigDecimal cutCost,
            BigDecimal totalCost,
            BigDecimal costPerPiece,
            BigDecimal wastePercentageApplied,
            BigDecimal makereadySheetsApplied
    ) {
    }

    public record QuoteOption(
            PriceCandidate price,
            CostBreakdown cost,
            PriceRule priceRule,
            List<String> warnings,
            String discardReason
    ) {
        public boolean accepted() {
            return discardReason == null || discardReason.isBlank();
        }
    }

    /**
     * @param quantity           piezas/unidades a producir
     * @param piecesPerSheet     piezas por pliego (&gt; 0)
     * @param cutValue           valor de corte por pliego (puede ser null → 0)
     * @param wasteSettings      rangos y defaults de merma de corte
     * @param wasteOverride      si no es null, debe caer en [min, max] de corte
     * @param ivaRate            IVA a sumar solo si {@code ivaIncluded=false} y no es descontable
     * @param ivaDeductible      si true y el precio no incluye IVA, no se suma IVA al costo
     */
    public static CostBreakdown calculate(
            int quantity,
            int piecesPerSheet,
            BigDecimal sheetValue,
            BigDecimal freightPerSheet,
            boolean ivaIncluded,
            BigDecimal cutValue,
            CompanyWasteSettings wasteSettings,
            BigDecimal wasteOverride,
            BigDecimal ivaRate,
            boolean ivaDeductible
    ) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }
        if (piecesPerSheet <= 0) {
            throw new IllegalArgumentException("Las piezas por pliego deben ser mayor que 0");
        }
        Objects.requireNonNull(wasteSettings, "La configuración de merma es obligatoria");

        BigDecimal percentage = resolveWastePercentage(wasteSettings, wasteOverride);
        BigDecimal makeready = WasteValuation.money(wasteSettings.getCutMakereadySheets());

        int netSheets = BigDecimal.valueOf(quantity)
                .divide(BigDecimal.valueOf(piecesPerSheet), 0, RoundingMode.CEILING)
                .intValue();
        BigDecimal wasteSheets = WasteValuation.plannedQuantity(netSheets, percentage, makeready);
        BigDecimal totalSheets = BigDecimal.valueOf(netSheets).add(wasteSheets);

        BigDecimal sheet = money(sheetValue);
        BigDecimal freight = money(freightPerSheet);
        BigDecimal unitLanded = sheet.add(freight);
        if (!ivaIncluded && !ivaDeductible && ivaRate != null && ivaRate.signum() > 0) {
            unitLanded = unitLanded.multiply(BigDecimal.ONE.add(
                    ivaRate.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)));
        }
        BigDecimal paperCost = money(totalSheets.multiply(unitLanded));
        // Misma lógica de Corte de papel: pliegos * valor de corte (soporta merma decimal).
        BigDecimal cutCost = money(totalSheets.multiply(money(cutValue)));
        BigDecimal totalCost = money(paperCost.add(cutCost));
        BigDecimal costPerPiece = money(totalCost.divide(BigDecimal.valueOf(quantity), 8, RoundingMode.HALF_UP));

        return new CostBreakdown(
                netSheets,
                wasteSheets,
                money(totalSheets),
                sheet,
                freight,
                paperCost,
                cutCost,
                totalCost,
                costPerPiece,
                percentage,
                makeready
        );
    }

    public static List<QuoteOption> selectAndQuote(
            List<PriceCandidate> candidates,
            PriceRule rule,
            int quantity,
            int piecesPerSheet,
            BigDecimal cutValue,
            CompanyWasteSettings wasteSettings,
            BigDecimal wasteOverride,
            Integer maxDeliveryDays,
            BigDecimal ivaRate,
            boolean ivaDeductible
    ) {
        Objects.requireNonNull(rule, "La regla de precio es obligatoria");
        List<QuoteOption> options = new ArrayList<>();
        if (candidates == null || candidates.isEmpty()) {
            return options;
        }

        for (PriceCandidate candidate : candidates) {
            List<String> warnings = new ArrayList<>();
            String discard = null;
            if (candidate.minPurchaseSheets() != null) {
                CostBreakdown preview = calculate(
                        quantity, piecesPerSheet,
                        candidate.sheetValue(), candidate.freightPerSheet(),
                        candidate.ivaIncluded(), cutValue, wasteSettings, wasteOverride,
                        ivaRate, ivaDeductible
                );
                if (preview.totalSheets().compareTo(BigDecimal.valueOf(candidate.minPurchaseSheets())) < 0) {
                    discard = "No cumple mínimo de compra (" + candidate.minPurchaseSheets() + " pliegos)";
                }
            }
            if (discard == null
                    && maxDeliveryDays != null
                    && candidate.deliveryDays() != null
                    && candidate.deliveryDays() > maxDeliveryDays) {
                discard = "Entrega en " + candidate.deliveryDays()
                        + " días supera el máximo " + maxDeliveryDays;
            }
            if (candidate.priceStale()) {
                warnings.add("Precio vencido (" + candidate.daysSincePriceDate() + " días desde price_date)");
            }

            CostBreakdown cost = discard == null
                    ? calculate(
                    quantity, piecesPerSheet,
                    candidate.sheetValue(), candidate.freightPerSheet(),
                    candidate.ivaIncluded(), cutValue, wasteSettings, wasteOverride,
                    ivaRate, ivaDeductible
            )
                    : null;
            options.add(new QuoteOption(candidate, cost, rule, List.copyOf(warnings), discard));
        }

        List<QuoteOption> accepted = options.stream().filter(QuoteOption::accepted).toList();
        if (accepted.isEmpty()) {
            return options.stream()
                    .sorted(Comparator.comparing(o -> o.price().landed()))
                    .toList();
        }

        return switch (rule) {
            case PREFERRED -> preferredFirst(accepted, options);
            case REPLACEMENT -> accepted.stream()
                    .sorted(Comparator.comparing((QuoteOption o) -> o.price().landed()).reversed()
                            .thenComparing(o -> o.price().supplierId()))
                    .toList();
            case BEST_COST -> accepted.stream()
                    .sorted(Comparator.comparing((QuoteOption o) -> o.cost().costPerPiece())
                            .thenComparing(o -> o.price().landed())
                            .thenComparing(o -> o.price().supplierId()))
                    .toList();
        };
    }

    private static List<QuoteOption> preferredFirst(List<QuoteOption> accepted, List<QuoteOption> all) {
        List<QuoteOption> preferred = accepted.stream().filter(o -> o.price().preferred()).toList();
        if (!preferred.isEmpty()) {
            return preferred;
        }
        // Sin preferido: caer a menor landed cost y advertir
        return accepted.stream()
                .sorted(Comparator.comparing((QuoteOption o) -> o.price().landed())
                        .thenComparing(o -> o.price().supplierId()))
                .map(o -> new QuoteOption(
                        o.price(),
                        o.cost(),
                        PriceRule.PREFERRED,
                        appendWarning(o.warnings(), "No hay proveedor preferido; se usa el de menor costo aterrizado"),
                        null
                ))
                .toList();
    }

    private static List<String> appendWarning(List<String> warnings, String extra) {
        List<String> copy = new ArrayList<>(warnings == null ? List.of() : warnings);
        copy.add(extra);
        return List.copyOf(copy);
    }

    private static BigDecimal resolveWastePercentage(
            CompanyWasteSettings settings,
            BigDecimal override
    ) {
        if (override == null) {
            return WasteValuation.percentage(null, settings.getCutWasteDefaultPercentage());
        }
        BigDecimal value = WasteValuation.percentage(override, settings.getCutWasteDefaultPercentage());
        if (value.compareTo(settings.getCutWasteMinPercentage()) < 0
                || value.compareTo(settings.getCutWasteMaxPercentage()) > 0) {
            throw new IllegalArgumentException(
                    "El porcentaje de merma debe estar entre "
                            + settings.getCutWasteMinPercentage().toPlainString()
                            + " y "
                            + settings.getCutWasteMaxPercentage().toPlainString()
            );
        }
        return value;
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
