package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.model.PaperTypeCutAssignment;
import com.inkcore.domain.papertype.model.PaperTypeSupplierAssignment;
import com.inkcore.domain.papertype.ports.out.PaperTypeRepositoryPort;
import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.DiscountType;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.productionorder.service.ProductionOrderCalculator;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class UpdateProductionOrderPaperCuttingUseCase {

    private final ProductionOrderSupport support;
    private final ProductionOrderOperatorsApplier operatorsApplier;
    private final PaperTypeRepositoryPort paperTypeRepository;
    private final CutLayoutRepositoryPort cutLayoutRepository;

    public UpdateProductionOrderPaperCuttingUseCase(
            ProductionOrderSupport support,
            ProductionOrderOperatorsApplier operatorsApplier,
            PaperTypeRepositoryPort paperTypeRepository,
            CutLayoutRepositoryPort cutLayoutRepository
    ) {
        this.support = support;
        this.operatorsApplier = operatorsApplier;
        this.paperTypeRepository = paperTypeRepository;
        this.cutLayoutRepository = cutLayoutRepository;
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

        List<PaperRow> rows = buildRows(order, companyId, command.paperRows());
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
            order.setCuttingCompletedAt(support.now());
        }
        order.setUpdatedAt(support.now());
        order.setUpdatedBy(userId);
        return support.repository().save(order);
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

        // Siempre persistir snapshots de catálogo cuando el front envía IDs:
        // también en cliente-suministra + ya cortado (rehidratación del GET).
        snapshotPaperAndCut(companyId, row, input);

        Integer goodSizes = resolveGoodSizes(plate, row);
        Integer sheets;
        if (clientPaper && Boolean.TRUE.equals(row.getPaperCut()) && !row.isMissingSupply()) {
            sheets = row.getDeliveredSheetsByClient();
            row.setCalculatedSheetsCount(sheets);
            row.setTotalPaperValue(BigDecimal.ZERO);
            row.setTotalCutValue(BigDecimal.ZERO);
            return;
        }

        sheets = ProductionOrderCalculator.calculateSheets(
                goodSizes,
                row.getPiecesPerSheet(),
                order.effectiveRoundingMargin()
        );
        if (row.isMissingSupply() && row.getMissingSheetsQuantity() != null) {
            sheets = row.getMissingSheetsQuantity();
        }
        row.setCalculatedSheetsCount(sheets);

        if (clientPaper && !row.isMissingSupply()) {
            // Cliente suministra y no viene cortado: se cobra corte, no papel de catálogo.
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
            UpdateProductionOrderPaperCuttingCommand.PaperRowInput input
    ) {
        if (input.paperTypeId() == null || input.paperTypeId().isBlank()) {
            return;
        }
        PaperType paper = paperTypeRepository.findById(input.paperTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("PAPER_TYPE_NOT_FOUND", "Tipo de papel no encontrado"));
        if (!companyId.equals(paper.getCompanyId())) {
            throw new ResourceNotFoundException("PAPER_TYPE_NOT_FOUND", "Tipo de papel no encontrado");
        }
        row.setPaperTypeId(paper.getPaperTypeId());
        row.setPaperName(paper.getName());
        row.setPaperSize(formatSize(paper.getWidth(), paper.getHeight(), paper.getUnit()));
        PaperTypeSupplierAssignment supplier = resolveSupplierAssignment(paper, input.supplierId());
        if (supplier != null) {
            row.setSupplierId(supplier.getSupplierId());
            row.setSheetValue(supplier.getSheetValue());
            row.setPackageUnit(supplier.getPackageUnit());
        } else {
            row.setSupplierId(null);
        }
        row.setCoated(paper.isCoated());

        if (input.cutLayoutId() == null || input.cutLayoutId().isBlank()) {
            return;
        }
        CutLayout cut = cutLayoutRepository.findById(input.cutLayoutId())
                .orElseThrow(() -> new ResourceNotFoundException("CUT_LAYOUT_NOT_FOUND", "Despiece no encontrado"));
        if (!companyId.equals(cut.getCompanyId())) {
            throw new ResourceNotFoundException("CUT_LAYOUT_NOT_FOUND", "Despiece no encontrado");
        }
        row.setCutLayoutId(cut.getCutLayoutId());
        row.setCutLayoutName(cut.getName());
        row.setCutLayoutSize(formatSize(cut.getWidth(), cut.getHeight(), cut.getUnit()));
        row.setPiecesPerSheet(cut.getPiecesPerSheet());
        BigDecimal cutValue = paper.getCutAssignments().stream()
                .filter(a -> cut.getCutLayoutId().equals(a.getCutLayoutId()))
                .map(PaperTypeCutAssignment::getCutValue)
                .findFirst()
                .orElse(null);
        row.setCutValue(cutValue);
    }

    private static PaperTypeSupplierAssignment resolveSupplierAssignment(
            PaperType paper,
            String supplierId
    ) {
        List<PaperTypeSupplierAssignment> assignments = paper.getSupplierAssignments();
        if (assignments == null || assignments.isEmpty()) {
            return null;
        }
        String trimmedId = blankToNull(supplierId);
        if (trimmedId != null) {
            return assignments.stream()
                    .filter(a -> trimmedId.equals(a.getSupplierId()))
                    .findFirst()
                    .orElseGet(() -> defaultSupplierAssignment(assignments));
        }
        return defaultSupplierAssignment(assignments);
    }

    private static PaperTypeSupplierAssignment defaultSupplierAssignment(
            List<PaperTypeSupplierAssignment> assignments
    ) {
        return assignments.stream()
                .max(Comparator.comparing(PaperTypeSupplierAssignment::getSheetValue))
                .orElse(assignments.get(0));
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
}
