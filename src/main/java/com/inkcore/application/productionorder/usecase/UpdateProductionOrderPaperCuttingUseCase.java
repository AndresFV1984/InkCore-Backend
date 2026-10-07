package com.inkcore.application.productionorder.usecase;

import com.inkcore.application.paper.usecase.PaperPricingSettings;
import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.paper.model.Paper;
import com.inkcore.domain.paper.model.PaperCutLayout;
import com.inkcore.domain.paper.model.PaperSupplierPrice;
import com.inkcore.domain.paper.model.PriceRule;
import com.inkcore.domain.paper.ports.out.PaperCutLayoutRepositoryPort;
import com.inkcore.domain.paper.ports.out.PaperRepositoryPort;
import com.inkcore.domain.paper.ports.out.PaperSupplierPriceRepositoryPort;
import com.inkcore.domain.paper.service.PaperCostCalculator;
import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.DiscountType;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.productionorder.service.ProductionOrderCalculator;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.wastesettings.model.CompanyWasteSettings;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class UpdateProductionOrderPaperCuttingUseCase {

    private final ProductionOrderSupport support;
    private final ProductionOrderOperatorsApplier operatorsApplier;
    private final PaperRepositoryPort paperRepository;
    private final PaperSupplierPriceRepositoryPort paperPriceRepository;
    private final PaperCutLayoutRepositoryPort paperCutLayoutRepository;
    private final CutLayoutRepositoryPort cutLayoutRepository;
    private final PaperPricingSettings pricingSettings;
    private final ProductionOrderCostingCoordinator costing;
    private final PaperRemnantStockSync remnantStockSync;

    public UpdateProductionOrderPaperCuttingUseCase(
            ProductionOrderSupport support,
            ProductionOrderOperatorsApplier operatorsApplier,
            PaperRepositoryPort paperRepository,
            PaperSupplierPriceRepositoryPort paperPriceRepository,
            PaperCutLayoutRepositoryPort paperCutLayoutRepository,
            CutLayoutRepositoryPort cutLayoutRepository,
            PaperPricingSettings pricingSettings,
            ProductionOrderCostingCoordinator costing,
            PaperRemnantStockSync remnantStockSync
    ) {
        this.support = support;
        this.operatorsApplier = operatorsApplier;
        this.paperRepository = paperRepository;
        this.paperPriceRepository = paperPriceRepository;
        this.paperCutLayoutRepository = paperCutLayoutRepository;
        this.cutLayoutRepository = cutLayoutRepository;
        this.pricingSettings = pricingSettings;
        this.costing = costing;
        this.remnantStockSync = remnantStockSync;
    }

    @Transactional
    public ProductionOrder execute(
            String productionOrderId,
            UpdateProductionOrderPaperCuttingCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        support.assertVersion(order, command.version());

        validate(command, order);

        if (command.clientSuppliesPaperDefault() != null) {
            order.setClientSuppliesPaperDefault(command.clientSuppliesPaperDefault());
        }
        if (command.roundingMargin() != null) {
            order.setRoundingMargin(command.roundingMargin());
        }

        List<PaperRow> previousRows = List.copyOf(order.getPaperRows());
        List<PaperRow> rows = buildRows(order, companyId, command.paperRows());
        applyRemnantUsageSnapshots(rows);
        var now = support.now();
        // Primero devolver lo previo, luego descontar lo nuevo (mismo remanente = neto correcto).
        remnantStockSync.releaseUsages(previousRows, now);
        remnantStockSync.applyUsages(companyId, rows, now);
        order.setPaperRows(rows);
        order.upsertStageDiscount(
                ProductionOrderStage.CUTTING,
                DiscountType.fromValue(command.discountType()),
                command.discountValue()
        );
        operatorsApplier.apply(
                order,
                companyId,
                command.operators(),
                command.operatorUserId(),
                ProductionOrderStage.CUTTING
        );
        if (Boolean.TRUE.equals(command.completed())) {
            order.setCuttingCompletedAt(now);
        }
        order.setUpdatedAt(now);
        order.setUpdatedBy(userId);
        ProductionOrder saved = support.repository().save(order);
        return costing.afterCutting(saved, command.machineUsages(), command.plannedMakereadyQuantity());
    }

    private static void applyRemnantUsageSnapshots(List<PaperRow> rows) {
        for (PaperRow row : rows) {
            String remnantId = blankToNull(row.getPaperRemnantId());
            if (remnantId == null) {
                row.setPaperRemnantId(null);
                row.setRemnantQuantityUsed(null);
                continue;
            }
            row.setPaperRemnantId(remnantId);
            Integer sheets = row.getCalculatedSheetsCount();
            if (sheets != null && sheets > 0) {
                row.setRemnantQuantityUsed(BigDecimal.valueOf(sheets).setScale(2, RoundingMode.HALF_UP));
            } else if (row.getTotalSheets() != null && row.getTotalSheets().compareTo(BigDecimal.ZERO) > 0) {
                row.setRemnantQuantityUsed(row.getTotalSheets().setScale(2, RoundingMode.HALF_UP));
            } else {
                row.setRemnantQuantityUsed(null);
            }
        }
    }

    private void validate(UpdateProductionOrderPaperCuttingCommand command, ProductionOrder order) {
        List<String> errors = new ArrayList<>();
        if (command.paperRows() == null) {
            return;
        }
        for (int i = 0; i < command.paperRows().size(); i++) {
            var row = command.paperRows().get(i);
            if (row.plateId() == null || order.findPlate(row.plateId()).isEmpty()) {
                errors.add("paperRows[" + i + "].plateId: plancha no encontrada en la orden");
            }
            if (row.cutRowKey() == null || row.cutRowKey().isBlank()) {
                errors.add("paperRows[" + i + "].cutRowKey: obligatorio");
            }
            if (row.clientSuppliesPaper() == null) {
                errors.add("paperRows[" + i + "].clientSuppliesPaper: obligatorio");
            }
            if (Boolean.TRUE.equals(row.isMissingSupply())
                    && (row.parentRowId() == null || row.parentRowId().isBlank())) {
                errors.add("paperRows[" + i + "].parentRowId: obligatorio cuando isMissingSupply=true");
            }
        }
        if (!errors.isEmpty()) {
            throw new ProductionOrderBusinessRuleException("Regla de negocio incumplida", errors);
        }
    }

    private List<PaperRow> buildRows(
            ProductionOrder order,
            String companyId,
            List<UpdateProductionOrderPaperCuttingCommand.PaperRowInput> inputs
    ) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        List<PaperRow> rows = new ArrayList<>();
        for (var input : inputs) {
            Plate plate = order.findPlate(input.plateId()).orElseThrow();
            PaperRow row = new PaperRow();
            if (input.paperRowId() != null && !input.paperRowId().isBlank()) {
                row.setPaperRowId(input.paperRowId().trim());
            }
            row.setCompanyId(companyId);
            row.setProductionOrderId(order.getProductionOrderId());
            row.setPlateId(plate.getPlateId());
            row.setParentRowId(blankToNull(input.parentRowId()));
            row.setCutRowKey(input.cutRowKey().trim());
            row.setMissingSupply(Boolean.TRUE.equals(input.isMissingSupply()));
            row.setMissingSheetsQuantity(input.missingSheetsQuantity());
            row.setClientSuppliesPaper(Boolean.TRUE.equals(input.clientSuppliesPaper()));
            row.setPaperCut(input.isPaperCut());
            row.setDeliveredSheetsByClient(input.deliveredSheetsByClient());
            row.setManualGoodSizes(input.manualGoodSizes());
            row.setManualSurplus(input.manualSurplus());
            row.setPaperRemnantId(blankToNull(input.paperRemnantId()));

            boolean companyCuts = !Boolean.TRUE.equals(row.getPaperCut());
            // Merma de OP: company_waste_settings (NO paper_cut_layouts.waste_percentage).
            row.setPlannedWastePercentage(costing.resolveCutPercentage(
                    companyId, input.plannedWastePercentage(), companyCuts));
            applyCatalogAndTotals(order, companyId, row, input);
            rows.add(row);
        }
        return rows;
    }

    private void applyCatalogAndTotals(
            ProductionOrder order,
            String companyId,
            PaperRow row,
            UpdateProductionOrderPaperCuttingCommand.PaperRowInput input
    ) {
        Plate plate = order.findPlate(row.getPlateId()).orElseThrow();
        boolean clientPaper = row.isClientSuppliesPaper();
        Integer goodSizes = resolveGoodSizes(plate, row);

        snapshotPaperAndCut(companyId, row, input, goodSizes);

        if (clientPaper && Boolean.TRUE.equals(row.getPaperCut()) && !row.isMissingSupply()) {
            Integer sheets = row.getDeliveredSheetsByClient();
            row.setCalculatedSheetsCount(sheets);
            row.setTotalPaperValue(BigDecimal.ZERO);
            row.setTotalCutValue(BigDecimal.ZERO);
            return;
        }

        if (row.getPaperId() == null
                || row.getPiecesPerSheet() == null
                || row.getPiecesPerSheet() <= 0
                || goodSizes == null
                || goodSizes <= 0) {
            applyLegacySheetTotals(order, row, goodSizes, clientPaper);
            return;
        }

        CompanyWasteSettings wasteSettings = costing.settingsFor(companyId);
        boolean chargePaper = !(clientPaper && !row.isMissingSupply());

        PaperCostCalculator.CostBreakdown breakdown = PaperCostCalculator.calculate(
                goodSizes,
                row.getPiecesPerSheet(),
                chargePaper ? row.getSheetValue() : BigDecimal.ZERO,
                chargePaper ? row.getFreightPerSheetSnapshot() : BigDecimal.ZERO,
                true,
                row.getCutValue(),
                wasteSettings,
                row.getPlannedWastePercentage(),
                pricingSettings.ivaRate(),
                pricingSettings.ivaDeductible()
        );

        if (row.isMissingSupply() && row.getMissingSheetsQuantity() != null) {
            BigDecimal missing = BigDecimal.valueOf(row.getMissingSheetsQuantity());
            row.setNetSheets(missing);
            row.setWasteSheets(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            row.setTotalSheets(missing.setScale(2, RoundingMode.HALF_UP));
            row.setCalculatedSheetsCount(row.getMissingSheetsQuantity());
            row.setTotalPaperValue(ProductionOrderCalculator.calculateLineTotal(
                    row.getMissingSheetsQuantity(), row.getSheetValue()));
            row.setTotalCutValue(ProductionOrderCalculator.calculateLineTotal(
                    row.getMissingSheetsQuantity(), row.getCutValue()));
            if (goodSizes > 0) {
                row.setCostPerPiece(money(row.getTotalPaperValue().add(row.getTotalCutValue())
                        .divide(BigDecimal.valueOf(goodSizes), 8, RoundingMode.HALF_UP)));
            }
            return;
        }

        row.setNetSheets(BigDecimal.valueOf(breakdown.netSheets()));
        row.setWasteSheets(breakdown.wasteSheets());
        row.setTotalSheets(breakdown.totalSheets());
        row.setPiecesPerSheetSnapshot(row.getPiecesPerSheet());
        row.setCostPerPiece(breakdown.costPerPiece());

        Integer sheets = breakdown.totalSheets().setScale(0, RoundingMode.CEILING).intValue();
        row.setCalculatedSheetsCount(sheets);

        if (!chargePaper) {
            row.setTotalPaperValue(BigDecimal.ZERO);
            row.setTotalCutValue(ProductionOrderCalculator.calculateLineTotal(sheets, row.getCutValue()));
        } else {
            row.setTotalPaperValue(ProductionOrderCalculator.calculateLineTotal(sheets, row.getSheetValue()));
            row.setTotalCutValue(ProductionOrderCalculator.calculateLineTotal(sheets, row.getCutValue()));
        }
    }

    private void applyLegacySheetTotals(
            ProductionOrder order,
            PaperRow row,
            Integer goodSizes,
            boolean clientPaper
    ) {
        Integer sheets = ProductionOrderCalculator.calculateSheets(
                goodSizes,
                row.getPiecesPerSheet(),
                order.effectiveRoundingMargin()
        );
        if (row.isMissingSupply() && row.getMissingSheetsQuantity() != null) {
            sheets = row.getMissingSheetsQuantity();
        }
        row.setCalculatedSheetsCount(sheets);
        if (clientPaper && !row.isMissingSupply()) {
            row.setTotalPaperValue(BigDecimal.ZERO);
            row.setTotalCutValue(ProductionOrderCalculator.calculateLineTotal(sheets, row.getCutValue()));
        } else {
            row.setTotalPaperValue(ProductionOrderCalculator.calculateLineTotal(sheets, row.getSheetValue()));
            row.setTotalCutValue(ProductionOrderCalculator.calculateLineTotal(sheets, row.getCutValue()));
        }
    }

    private void snapshotPaperAndCut(
            String companyId,
            PaperRow row,
            UpdateProductionOrderPaperCuttingCommand.PaperRowInput input,
            Integer goodSizes
    ) {
        String paperId = blankToNull(input.paperId());
        if (paperId == null) {
            return;
        }

        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("PAPER_NOT_FOUND", "Papel no encontrado"));
        if (!companyId.equals(paper.getCompanyId())) {
            throw new ResourceNotFoundException("PAPER_NOT_FOUND", "Papel no encontrado");
        }

        row.setPaperId(paper.getPaperId());
        row.setPaperName(paper.getName());
        row.setPaperSize(formatSize(paper.getWidth(), paper.getHeight(), paper.getUnit()));
        row.setCoated(paper.isCoated());
        row.setCoatedSnapshot(paper.isCoated());

        applyCutLayoutSnapshot(companyId, paper.getPaperId(), row, input);

        List<PaperSupplierPrice> prices = paperPriceRepository.findByPaperId(companyId, paper.getPaperId());
        applyPriceSnapshot(companyId, row, input, prices, goodSizes);
    }

    private void applyCutLayoutSnapshot(
            String companyId,
            String paperId,
            PaperRow row,
            UpdateProductionOrderPaperCuttingCommand.PaperRowInput input
    ) {
        String paperCutLayoutId = blankToNull(input.paperCutLayoutId());
        String cutLayoutId = blankToNull(input.cutLayoutId());
        if (paperCutLayoutId == null && cutLayoutId == null) {
            return;
        }

        PaperCutLayout paperCut = null;
        if (paperCutLayoutId != null) {
            paperCut = paperCutLayoutRepository.findById(paperCutLayoutId)
                    .filter(l -> companyId.equals(l.getCompanyId()) && paperId.equals(l.getPaperId()))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "PAPER_CUT_LAYOUT_NOT_FOUND", "Despiece del papel no encontrado"));
            cutLayoutId = paperCut.getCutLayoutId();
        } else {
            String finalCutLayoutId = cutLayoutId;
            paperCut = paperCutLayoutRepository.findByPaperId(companyId, paperId).stream()
                    .filter(l -> finalCutLayoutId.equals(l.getCutLayoutId()))
                    .findFirst()
                    .orElse(null);
        }

        CutLayout cut = cutLayoutRepository.findById(cutLayoutId)
                .orElseThrow(() -> new ResourceNotFoundException("CUT_LAYOUT_NOT_FOUND", "Despiece no encontrado"));
        if (!companyId.equals(cut.getCompanyId())) {
            throw new ResourceNotFoundException("CUT_LAYOUT_NOT_FOUND", "Despiece no encontrado");
        }
        row.setCutLayoutId(cut.getCutLayoutId());
        row.setCutLayoutName(cut.getName());
        row.setCutLayoutSize(formatSize(cut.getWidth(), cut.getHeight(), cut.getUnit()));
        // pieces_per_sheet y cut_value siempre del catálogo cut_layouts.
        row.setPiecesPerSheet(cut.getPiecesPerSheet());
        row.setPiecesPerSheetSnapshot(cut.getPiecesPerSheet());
        row.setCutValue(cut.getCutValue());
    }

    private void applyPriceSnapshot(
            String companyId,
            PaperRow row,
            UpdateProductionOrderPaperCuttingCommand.PaperRowInput input,
            List<PaperSupplierPrice> prices,
            Integer goodSizes
    ) {
        if (prices == null || prices.isEmpty()) {
            row.setSupplierId(null);
            return;
        }

        String priceRuleRaw = blankToNull(input.priceRule());
        if (priceRuleRaw != null
                && goodSizes != null
                && goodSizes > 0
                && row.getPiecesPerSheet() != null
                && row.getPiecesPerSheet() > 0) {
            PriceRule rule = PriceRule.fromApiValue(priceRuleRaw);
            CompanyWasteSettings wasteSettings = costing.settingsFor(companyId);
            List<PaperCostCalculator.PriceCandidate> candidates = toCandidates(prices);
            List<PaperCostCalculator.QuoteOption> options = PaperCostCalculator.selectAndQuote(
                    candidates,
                    rule,
                    goodSizes,
                    row.getPiecesPerSheet(),
                    row.getCutValue(),
                    wasteSettings,
                    row.getPlannedWastePercentage(),
                    null,
                    pricingSettings.ivaRate(),
                    pricingSettings.ivaDeductible()
            );
            PaperCostCalculator.QuoteOption best = options.stream()
                    .filter(PaperCostCalculator.QuoteOption::accepted)
                    .findFirst()
                    .orElse(options.isEmpty() ? null : options.get(0));
            if (best != null) {
                PaperSupplierPrice selected = prices.stream()
                        .filter(p -> p.getSupplierId().equals(best.price().supplierId()))
                        .findFirst()
                        .orElse(null);
                applySelectedPrice(row, selected, rule);
                return;
            }
        }

        PaperSupplierPrice selected = resolvePrice(prices, input.supplierId());
        PriceRule inferred = selected != null && selected.isPreferred()
                ? PriceRule.PREFERRED
                : PriceRule.REPLACEMENT;
        applySelectedPrice(row, selected, priceRuleRaw == null ? inferred : PriceRule.fromApiValue(priceRuleRaw));
    }

    private List<PaperCostCalculator.PriceCandidate> toCandidates(List<PaperSupplierPrice> prices) {
        LocalDate today = LocalDate.now(support.clock());
        List<PaperCostCalculator.PriceCandidate> candidates = new ArrayList<>();
        for (PaperSupplierPrice price : prices) {
            if (!price.isState()) {
                continue;
            }
            int daysSince = (int) ChronoUnit.DAYS.between(price.getPriceDate(), today);
            boolean stale = daysSince > pricingSettings.priceStaleDays();
            BigDecimal landed = price.getLandedCostPerSheet() != null
                    ? price.getLandedCostPerSheet()
                    : price.getSheetValue().add(price.getFreightPerSheet());
            candidates.add(new PaperCostCalculator.PriceCandidate(
                    price.getSupplierId(),
                    price.getSupplierId(),
                    price.getSheetValue(),
                    price.getFreightPerSheet(),
                    landed,
                    true,
                    price.getMinPurchaseSheets(),
                    price.getDeliveryDays(),
                    price.isPreferred(),
                    stale,
                    daysSince
            ));
        }
        return candidates;
    }

    private static void applySelectedPrice(PaperRow row, PaperSupplierPrice selected, PriceRule rule) {
        row.setPriceRule(rule);
        if (selected == null) {
            row.setSupplierId(null);
            return;
        }
        row.setSupplierId(selected.getSupplierId());
        row.setSheetValue(selected.getSheetValue());
        row.setPackageUnit(selected.getPackageUnit());
        row.setFreightPerSheetSnapshot(selected.getFreightPerSheet());
        row.setPriceDateSnapshot(selected.getPriceDate());
    }

    private static PaperSupplierPrice resolvePrice(List<PaperSupplierPrice> prices, String supplierId) {
        List<PaperSupplierPrice> active = prices.stream().filter(PaperSupplierPrice::isState).toList();
        if (active.isEmpty()) {
            return null;
        }
        String trimmedId = blankToNull(supplierId);
        if (trimmedId != null) {
            return active.stream()
                    .filter(p -> trimmedId.equals(p.getSupplierId()))
                    .findFirst()
                    .orElseGet(() -> preferredOrCheapest(active));
        }
        return preferredOrCheapest(active);
    }

    private static PaperSupplierPrice preferredOrCheapest(List<PaperSupplierPrice> prices) {
        return prices.stream()
                .filter(PaperSupplierPrice::isPreferred)
                .findFirst()
                .orElseGet(() -> prices.stream()
                        .min(Comparator.comparing(PaperSupplierPrice::getSheetValue)
                                .thenComparing(PaperSupplierPrice::getSupplierId))
                        .orElse(prices.get(0)));
    }

    private static Integer resolveGoodSizes(Plate plate, PaperRow row) {
        if (row.getManualGoodSizes() != null) {
            return row.getManualGoodSizes();
        }
        return plate.getGoodSizes();
    }

    private static String formatSize(BigDecimal width, BigDecimal height, String unit) {
        if (width == null || height == null) {
            return null;
        }
        String u = unit == null ? "" : unit;
        return width.stripTrailingZeros().toPlainString() + "x"
                + height.stripTrailingZeros().toPlainString() + u;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
