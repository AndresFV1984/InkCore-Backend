package com.inkcore.application.productionorder.usecase;

import com.inkcore.application.inkestimateasset.InkEstimateAssetRelocationService;
import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.DiscountType;
import com.inkcore.domain.productionorder.model.FlipType;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.PrintConfig;
import com.inkcore.domain.productionorder.model.PrintEntry;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.productionorder.service.ProductionOrderCalculator;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.thousandrate.model.ThousandRate;
import com.inkcore.domain.thousandrate.ports.out.ThousandRateRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UpdateProductionOrderPrintingUseCase {

    private final ProductionOrderSupport support;
    private final ThousandRateRepositoryPort thousandRateRepository;
    private final InkEstimateAssetRelocationService inkEstimateAssetRelocation;

    public UpdateProductionOrderPrintingUseCase(
            ProductionOrderSupport support,
            ThousandRateRepositoryPort thousandRateRepository,
            InkEstimateAssetRelocationService inkEstimateAssetRelocation
    ) {
        this.support = support;
        this.thousandRateRepository = thousandRateRepository;
        this.inkEstimateAssetRelocation = inkEstimateAssetRelocation;
    }

    @Transactional
    public ProductionOrder execute(
            String productionOrderId,
            UpdateProductionOrderPrintingCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        support.assertVersion(order, command.version());

        List<PrintConfig> prints = buildPrints(order, companyId, userId, command.prints());
        order.setPrints(prints);
        order.upsertOperator(ProductionOrderStage.PRINTING, command.operatorUserId());
        if (Boolean.TRUE.equals(command.completed())) {
            order.setPrintingCompletedAt(support.now());
        }
        order.setUpdatedAt(support.now());
        order.setUpdatedBy(userId);
        return support.repository().save(order);
    }

    private List<PrintConfig> buildPrints(
            ProductionOrder order,
            String companyId,
            String userId,
            List<UpdateProductionOrderPrintingCommand.PrintInput> inputs
    ) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        List<String> errors = new ArrayList<>();
        Set<String> plateIds = new HashSet<>();
        List<PrintConfig> prints = new ArrayList<>();

        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            if (input.plateId() == null || order.findPlate(input.plateId()).isEmpty()) {
                errors.add("prints[" + i + "].plateId: plancha no encontrada");
                continue;
            }
            if (!plateIds.add(input.plateId())) {
                errors.add("prints[" + i + "].plateId: solo se permite un registro de impresión por plancha");
            }
            Plate plate = order.findPlate(input.plateId()).orElseThrow();
            PrintConfig print = new PrintConfig();
            if (input.printId() != null && !input.printId().isBlank()) {
                print.setPrintId(input.printId().trim());
            }
            print.setCompanyId(companyId);
            print.setProductionOrderId(order.getProductionOrderId());
            print.setPlateId(plate.getPlateId());
            print.setClientSuppliesSherpa(input.clientSuppliesSherpa());
            print.setSherpaTestPrice(input.sherpaTestPrice());
            print.setMachineOutputValue(input.machineOutputValue());
            print.setInkEstimation(inkEstimateAssetRelocation.finalizeForPrint(
                    companyId,
                    userId,
                    order.getProductionOrderId(),
                    plate.getPlateId(),
                    input.inkEstimation(),
                    persistedInkEstimation(order, plate.getPlateId())
            ));
            print.setPrintingDiscountType(DiscountType.fromValue(input.printingDiscountType()));
            print.setPrintingDiscountValue(input.printingDiscountValue());
            print.setCompleted(Boolean.TRUE.equals(input.completed()));
            print.setEntries(buildEntries(companyId, print.getPrintId(), plate, input.entries(), errors, i));
            prints.add(print);
        }

        if (!errors.isEmpty()) {
            throw new ProductionOrderBusinessRuleException("Regla de negocio incumplida", errors);
        }
        return prints;
    }

    private static Map<String, Object> persistedInkEstimation(ProductionOrder order, String plateId) {
        if (order.getPrints() == null || plateId == null) {
            return null;
        }
        for (PrintConfig existing : order.getPrints()) {
            if (plateId.equals(existing.getPlateId())) {
                return existing.getInkEstimation();
            }
        }
        return null;
    }

    private List<PrintEntry> buildEntries(
            String companyId,
            String printId,
            Plate plate,
            List<UpdateProductionOrderPrintingCommand.PrintEntryInput> inputs,
            List<String> errors,
            int printIndex
    ) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        List<PrintEntry> entries = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            int shots = Objects.requireNonNullElse(input.shotsInkCount(), 0);
            int reverse = Objects.requireNonNullElse(input.reverseInkCount(), 0);
            if (!ProductionOrderCalculator.inkCountsMatchColors(shots, reverse, plate.getColors())) {
                errors.add("prints[" + printIndex + "].entries[" + i
                        + "]: la suma de tintas tiro+retiro debe igualar colors de la plancha");
            }

            FlipType pantoneFlip = FlipType.fromValue(input.pantoneFlipType());
            List<String> shotsInks = input.shotsInks() == null ? List.of() : input.shotsInks();
            List<String> reverseInks = input.reverseInks() == null ? List.of() : input.reverseInks();
            if (pantoneFlip != FlipType.NO_FLIP && !hasPantoneInk(shotsInks) && !hasPantoneInk(reverseInks)) {
                errors.add("prints[" + printIndex + "].entries[" + i
                        + "]: volteo Pantone requiere tintas Pantone en tiro o retiro");
            }

            PrintEntry entry = new PrintEntry();
            if (input.printEntryId() != null && !input.printEntryId().isBlank()) {
                entry.setPrintEntryId(input.printEntryId().trim());
            }
            entry.setCompanyId(companyId);
            entry.setPrintId(printId);
            entry.setShotsInkCount(shots);
            entry.setShotsInks(shotsInks);
            entry.setReverseInkCount(reverse);
            entry.setReverseInks(reverseInks);
            entry.setBasicFlipType(FlipType.fromValue(input.basicFlipType()));
            entry.setPantoneFlipType(pantoneFlip);
            entry.setClientSuppliesPantoneInk(input.clientSuppliesPantoneInk());
            entry.setPantoneInkChargePrice(input.pantoneInkChargePrice());

            applyThousandRate(companyId, entry, true, input.basicThousandRateId(), plate.getGoodSizes());
            applyThousandRate(companyId, entry, false, input.pantoneThousandRateId(), plate.getGoodSizes());
            entries.add(entry);
        }
        return entries;
    }

    private void applyThousandRate(
            String companyId,
            PrintEntry entry,
            boolean basic,
            String rateId,
            Integer goodSizes
    ) {
        if (rateId == null || rateId.isBlank()) {
            return;
        }
        ThousandRate rate = thousandRateRepository.findById(rateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "THOUSAND_RATE_NOT_FOUND", "Tarifa por millar no encontrada"));
        if (!companyId.equals(rate.getCompanyId())) {
            throw new ResourceNotFoundException("THOUSAND_RATE_NOT_FOUND", "Tarifa por millar no encontrada");
        }
        BigDecimal thousands = ProductionOrderCalculator.calculateThousands(goodSizes, rate);
        if (basic) {
            entry.setBasicThousandRateId(rate.getThousandRateId());
            entry.setBasicRateName(rate.getName());
            entry.setBasicRatePrice(rate.getPrice());
            entry.setBasicRateGripperFlipPrice(rate.getGripperFlipPrice());
            entry.setBasicRateSquareFlipPrice(rate.getSquareFlipPrice());
            entry.setBasicCalculatedThousands(thousands);
            BigDecimal flipPrice = ProductionOrderCalculator.resolveFlipPrice(
                    entry.getBasicFlipType(),
                    rate.getPrice(),
                    rate.getGripperFlipPrice(),
                    rate.getSquareFlipPrice()
            );
            entry.setBasicPrintingPrice(ProductionOrderCalculator.calculatePrintingPrice(thousands, flipPrice));
        } else {
            entry.setPantoneThousandRateId(rate.getThousandRateId());
            entry.setPantoneRateName(rate.getName());
            entry.setPantoneRatePrice(rate.getPrice());
            entry.setPantoneRateGripperFlipPrice(rate.getGripperFlipPrice());
            entry.setPantoneRateSquareFlipPrice(rate.getSquareFlipPrice());
            entry.setPantoneCalculatedThousands(thousands);
            BigDecimal flipPrice = ProductionOrderCalculator.resolveFlipPrice(
                    entry.getPantoneFlipType(),
                    rate.getPrice(),
                    rate.getGripperFlipPrice(),
                    rate.getSquareFlipPrice()
            );
            entry.setPantonePrintingPrice(ProductionOrderCalculator.calculatePrintingPrice(thousands, flipPrice));
        }
    }

    private static boolean hasPantoneInk(List<String> inks) {
        for (String ink : inks) {
            if (ink != null && ink.toLowerCase(Locale.ROOT).contains("pantone")) {
                return true;
            }
        }
        return false;
    }
}
