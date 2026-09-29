package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.machine.model.MachineType;
import com.inkcore.domain.machine.ports.out.MachineRepositoryPort;
import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.MachineUsage;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PostpressType;
import com.inkcore.domain.productionorder.model.PrintConfig;
import com.inkcore.domain.productionorder.model.PrintEntry;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.WasteReason;
import com.inkcore.domain.productionorder.model.WasteRecord;
import com.inkcore.domain.productionorder.ports.out.CostSummaryRepositoryPort;
import com.inkcore.domain.productionorder.ports.out.MachineUsageRepositoryPort;
import com.inkcore.domain.productionorder.ports.out.WasteRecordRepositoryPort;
import com.inkcore.domain.productionorder.service.ProductionOrderCalculator;
import com.inkcore.domain.productionorder.service.WasteMakeready;
import com.inkcore.domain.productionorder.service.WasteValuation;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.station.model.StationPhase;
import com.inkcore.domain.wastesettings.model.CompanyWasteSettings;
import com.inkcore.domain.wastesettings.ports.out.CompanyWasteSettingsRepositoryPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Costeo de máquina y merma del wizard y del cierre de fase.
 * No escribe las columnas derivadas de {@code production_order_cost_summary}:
 * eso lo hace el trigger. Solo fija {@code quoted_price}.
 */
@Component
public class ProductionOrderCostingCoordinator {

    public static final String PHASE_PREPRESS = StationPhase.PREPRENSA.getApiValue();
    public static final String PHASE_CUTTING = StationPhase.CORTE_PAPEL.getApiValue();
    public static final String PHASE_PRINTING = StationPhase.IMPRESION.getApiValue();
    public static final String PHASE_FINISHED = StationPhase.TERMINADOS.getApiValue();
    public static final String PHASE_FINISHING = StationPhase.ACABADOS.getApiValue();

    public static final String MERMA_CORTE = "merma_corte";
    public static final String MERMA_OPERATIVA = "merma_operativa";
    public static final String DESPERDICIO = "desperdicio";

    private final MachineRepositoryPort machineRepository;
    private final MachineUsageRepositoryPort machineUsageRepository;
    private final WasteRecordRepositoryPort wasteRecordRepository;
    private final CompanyWasteSettingsRepositoryPort wasteSettingsRepository;
    private final CostSummaryRepositoryPort costSummaryRepository;
    private final Clock clock;

    public ProductionOrderCostingCoordinator(
            MachineRepositoryPort machineRepository,
            MachineUsageRepositoryPort machineUsageRepository,
            WasteRecordRepositoryPort wasteRecordRepository,
            CompanyWasteSettingsRepositoryPort wasteSettingsRepository,
            CostSummaryRepositoryPort costSummaryRepository,
            Clock clock
    ) {
        this.machineRepository = machineRepository;
        this.machineUsageRepository = machineUsageRepository;
        this.wasteRecordRepository = wasteRecordRepository;
        this.wasteSettingsRepository = wasteSettingsRepository;
        this.costSummaryRepository = costSummaryRepository;
        this.clock = clock;
    }

    public CompanyWasteSettings settingsFor(String companyId) {
        return wasteSettingsRepository.findByCompanyId(companyId)
                .orElseGet(() -> CompanyWasteSettings.initialSuggestion(companyId));
    }

    public BigDecimal resolveCutPercentage(String companyId, BigDecimal requested, boolean companyCutsThePaper) {
        if (!companyCutsThePaper) {
            return WasteValuation.money(BigDecimal.ZERO);
        }
        return WasteValuation.percentage(requested, settingsFor(companyId).getCutWasteDefaultPercentage());
    }

    public ProductionOrder afterPrepress(
            ProductionOrder order,
            List<MachineUsageInput> machines,
            BigDecimal plannedWastePercentage
    ) {
        machineUsageRepository.deleteByProductionOrderIdAndPhaseNot(order.getProductionOrderId(), PHASE_PREPRESS);
        wasteRecordRepository.deleteByProductionOrderId(order.getProductionOrderId());
        applyMachines(order, PHASE_PREPRESS, machines);
        syncStepWaste(order, PHASE_PREPRESS, "plancha", plannedWastePercentage);
        return finishPhase(order, PHASE_PREPRESS);
    }

    public ProductionOrder afterCutting(
            ProductionOrder order,
            List<MachineUsageInput> machines,
            BigDecimal plannedMakereadyQuantity
    ) {
        syncCutWaste(order, plannedMakereadyQuantity);
        applyMachines(order, PHASE_CUTTING, machines);
        return finish(order);
    }

    public ProductionOrder afterPrinting(
            ProductionOrder order,
            List<MachineUsageInput> machines,
            BigDecimal plannedOperationalWastePercentage,
            BigDecimal plannedMakereadyQuantity
    ) {
        syncOperationalWaste(order, plannedOperationalWastePercentage, plannedMakereadyQuantity);
        applyMachines(order, PHASE_PRINTING, machines);
        return finish(order);
    }

    public ProductionOrder afterPhase(
            ProductionOrder order,
            String phase,
            List<MachineUsageInput> machines,
            BigDecimal plannedWastePercentage
    ) {
        applyMachines(order, phase, machines);
        syncStepWaste(order, phase, "acabado", plannedWastePercentage);
        return finishPhase(order, phase);
    }

    public void capturePhaseClose(
            ProductionOrder order,
            String phase,
            BigDecimal actualQuantity,
            Integer actualSetupMinutes,
            Integer actualRunMinutes,
            String machineId,
            String wasteReason
    ) {
        if (!isMachinePhase(phase)) {
            return;
        }
        WasteReason reason = WasteReason.fromApiValue(wasteReason);
        if (actualQuantity == null && actualSetupMinutes == null && actualRunMinutes == null && reason == null) {
            return;
        }
        if (actualQuantity != null && actualQuantity.signum() < 0) {
            throw new ProductionOrderBusinessRuleException("La cantidad de merma real no puede ser negativa");
        }
        if (actualQuantity != null && reason == null) {
            throw new ProductionOrderBusinessRuleException("El motivo de la merma es obligatorio cuando se informa la cantidad real");
        }
        applyActualMinutes(order, phase, actualSetupMinutes, actualRunMinutes, machineId);
        if (actualQuantity != null) {
            applyActualWaste(order, phase, actualQuantity, reason.getLabel());
        }
    }

    public WasteRecord registerReprint(
            ProductionOrder order,
            String phase,
            BigDecimal quantity,
            String wasteReason,
            String machineId
    ) {
        if (!isMachinePhase(phase)) {
            throw new ProductionOrderBusinessRuleException(
                    "phase: preprensa | corte-papel | impresion | terminados | acabados");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new ProductionOrderBusinessRuleException("La cantidad de retrabajo debe ser mayor que cero");
        }
        WasteReason reason = WasteReason.fromApiValue(wasteReason);
        if (reason == null) {
            throw new ProductionOrderBusinessRuleException("El motivo del retrabajo es obligatorio");
        }
        assertMachineOnPhase(order, phase, machineId);
        LocalDateTime now = LocalDateTime.now(clock);
        WasteRecord record = new WasteRecord();
        record.setCompanyId(order.getCompanyId());
        record.setProductionOrderId(order.getProductionOrderId());
        record.setPhase(phase);
        record.setWasteCategory(DESPERDICIO);
        record.setWasteOrigin(WasteMakeready.ORIGIN_RETRABAJO);
        record.setMaterialType(materialTypeFor(phase));
        record.setPlannedQuantity(WasteValuation.money(BigDecimal.ZERO));
        record.setActualQuantity(WasteValuation.money(quantity));
        record.setUnitCostSnapshot(excessUnitCost(order, phase));
        record.setNote(reason.getLabel());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return wasteRecordRepository.save(record);
    }

    public void syncQuotedPrice(ProductionOrder order) {
        costSummaryRepository.upsertQuotedPrice(
                order.getProductionOrderId(),
                order.getCompanyId(),
                ProductionOrderCalculator.calculateTotalToCharge(order)
        );
    }

    public void attach(ProductionOrder order) {
        String companyId = order.getCompanyId();
        String orderId = order.getProductionOrderId();
        order.setMachineUsages(machineUsageRepository.findByProductionOrderId(companyId, orderId));
        order.setWasteRecords(wasteRecordRepository.findByProductionOrderId(companyId, orderId));
        WasteMakeready.restore(order);
    }

    private ProductionOrder finish(ProductionOrder order) {
        attach(order);
        syncQuotedPrice(order);
        return order;
    }

    private ProductionOrder finishPhase(ProductionOrder order, String phase) {
        ProductionOrder finished = finish(order);
        finished.setPhaseEstimatedMachineCost(sumEstimatedMachineCost(finished, phase));
        finished.setPhaseEstimatedWasteCost(sumPlannedWasteCost(finished, phase));
        return finished;
    }

    private void applyMachines(ProductionOrder order, String phase, List<MachineUsageInput> inputs) {
        if (inputs == null) {
            return;
        }
        MachineType expected = MachineType.fromApiValue(phase);
        if (inputs.isEmpty()) {
            machineUsageRepository.deleteByProductionOrderIdAndPhase(order.getProductionOrderId(), phase);
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Set<String> seen = new HashSet<>();
        List<MachineUsage> usages = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            MachineUsageInput input = inputs.get(i);
            if (input == null || input.machineId() == null || input.machineId().isBlank()) {
                throw new ProductionOrderBusinessRuleException(
                        "machineUsages[" + i + "].machineId: obligatorio");
            }
            String machineId = input.machineId().trim();
            if (!seen.add(machineId)) {
                throw new ProductionOrderBusinessRuleException(
                        "machineUsages[" + i + "]: la máquina está repetida en la fase");
            }
            Machine machine = machineRepository.findById(machineId)
                    .filter(candidate -> order.getCompanyId().equals(candidate.getCompanyId()) && candidate.isState())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "MACHINE_NOT_FOUND",
                            "Máquina no encontrada o inactiva"
                    ));
            if (machine.getMachineType() != expected) {
                throw new ProductionOrderBusinessRuleException(
                        "La máquina " + machine.getName() + " no corresponde a la fase " + phase);
            }
            if (machine.getCostPerHour() == null) {
                throw new ProductionOrderBusinessRuleException(
                        "La máquina " + machine.getName() + " no tiene costo/hora calculado");
            }
            MachineUsage usage = new MachineUsage();
            usage.setCompanyId(order.getCompanyId());
            usage.setProductionOrderId(order.getProductionOrderId());
            usage.setPhase(phase);
            usage.setMachineId(machine.getMachineId());
            usage.setMachineNameSnapshot(machine.getName());
            usage.setCostPerHourSnapshot(machine.getCostPerHour());
            usage.setEstimatedSetupMinutes(nonNegativeMinutes(input.estimatedSetupMinutes(), "estimatedSetupMinutes"));
            usage.setEstimatedRunMinutes(nonNegativeMinutes(input.estimatedRunMinutes(), "estimatedRunMinutes"));
            usage.setCreatedAt(now);
            usage.setUpdatedAt(now);
            usages.add(usage);
        }
        machineUsageRepository.replacePhase(order.getProductionOrderId(), phase, usages);
    }

    private void syncCutWaste(ProductionOrder order, BigDecimal requestedMakeready) {
        BigDecimal makeready = resolveMakeready(
                requestedMakeready,
                settingsFor(order.getCompanyId()).getCutMakereadySheets()
        );
        LocalDateTime now = LocalDateTime.now(clock);
        List<WasteRecord> existing = wasteRecordRepository.findByProductionOrderPhaseAndCategory(
                order.getCompanyId(), order.getProductionOrderId(), PHASE_CUTTING, MERMA_CORTE);
        Map<String, WasteRecord> byPaperRow = new LinkedHashMap<>();
        for (WasteRecord record : existing) {
            if (record.getPaperRowId() != null) {
                byPaperRow.put(record.getPaperRowId(), record);
            }
        }
        Set<String> kept = new HashSet<>();
        boolean carrier = true;
        for (PaperRow row : order.getPaperRows()) {
            WasteRecord record = byPaperRow.get(row.getPaperRowId());
            if (record == null) {
                record = new WasteRecord();
                record.setCreatedAt(now);
            }
            record.setCompanyId(order.getCompanyId());
            record.setProductionOrderId(order.getProductionOrderId());
            record.setPhase(PHASE_CUTTING);
            record.setWasteCategory(MERMA_CORTE);
            record.setWasteOrigin(WasteMakeready.ORIGIN_EXCESO);
            record.setMaterialType("papel");
            record.setPaperRowId(row.getPaperRowId());
            BigDecimal fixed = carrier ? makeready : WasteValuation.money(BigDecimal.ZERO);
            record.setPlannedQuantity(WasteValuation.plannedQuantity(
                    row.getCalculatedSheetsCount(), row.getPlannedWastePercentage(), fixed));
            if (carrier) {
                record.setNote(WasteMakeready.note(makeready));
                carrier = false;
            } else if (WasteMakeready.read(record.getNote()) != null) {
                record.setNote(null);
            }
            BigDecimal unit = row.isClientSuppliesPaper()
                    ? WasteValuation.money(BigDecimal.ZERO)
                    : WasteValuation.money(row.getSheetValue());
            record.setUnitCostSnapshot(unit);
            record.setUpdatedAt(now);
            WasteRecord saved = wasteRecordRepository.save(record);
            kept.add(saved.getWasteRecordId());
        }
        List<String> obsolete = existing.stream()
                .map(WasteRecord::getWasteRecordId)
                .filter(id -> !kept.contains(id))
                .toList();
        wasteRecordRepository.deleteByIds(obsolete);
    }

    private void syncOperationalWaste(
            ProductionOrder order,
            BigDecimal requestedPercentage,
            BigDecimal requestedMakeready
    ) {
        BigDecimal makeready = resolveMakeready(
                requestedMakeready,
                settingsFor(order.getCompanyId()).getOperationalMakereadySheets()
        );
        BigDecimal percentage = WasteValuation.percentage(
                requestedPercentage,
                settingsFor(order.getCompanyId()).getOperationalWasteDefaultPercentage()
        );
        order.setPlannedOperationalWastePercentage(percentage);
        int sheets = totalSheets(order);
        LocalDateTime now = LocalDateTime.now(clock);
        List<WasteRecord> existing = wasteRecordRepository.findByProductionOrderPhaseAndCategory(
                order.getCompanyId(), order.getProductionOrderId(), PHASE_PRINTING, MERMA_OPERATIVA);
        WasteRecord record = existing.isEmpty() ? new WasteRecord() : existing.get(0);
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(now);
        }
        record.setCompanyId(order.getCompanyId());
        record.setProductionOrderId(order.getProductionOrderId());
        record.setPhase(PHASE_PRINTING);
        record.setWasteCategory(MERMA_OPERATIVA);
        record.setWasteOrigin(WasteMakeready.ORIGIN_EXCESO);
        record.setMaterialType("papel");
        record.setPaperRowId(null);
        record.setPlannedQuantity(WasteValuation.plannedQuantity(sheets, percentage, makeready));
        record.setNote(WasteMakeready.note(makeready));
        record.setUnitCostSnapshot(averageSheetValue(order));
        record.setUpdatedAt(now);
        WasteRecord saved = wasteRecordRepository.save(record);
        List<String> extras = existing.stream()
                .map(WasteRecord::getWasteRecordId)
                .filter(id -> !id.equals(saved.getWasteRecordId()))
                .toList();
        wasteRecordRepository.deleteByIds(extras);
    }

    private void syncStepWaste(
            ProductionOrder order,
            String phase,
            String materialType,
            BigDecimal requestedPercentage
    ) {
        BigDecimal percentage;
        try {
            percentage = WasteValuation.percentage(
                    requestedPercentage,
                    settingsFor(order.getCompanyId()).defaultPercentage(phase)
            );
        } catch (IllegalArgumentException ex) {
            throw new ProductionOrderBusinessRuleException(ex.getMessage());
        }
        int units = stepUnits(order, phase);
        LocalDateTime now = LocalDateTime.now(clock);
        List<WasteRecord> existing = wasteRecordRepository.findByProductionOrderPhaseAndCategory(
                order.getCompanyId(), order.getProductionOrderId(), phase, MERMA_OPERATIVA);
        WasteRecord record = existing.isEmpty() ? new WasteRecord() : existing.get(0);
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(now);
        }
        record.setCompanyId(order.getCompanyId());
        record.setProductionOrderId(order.getProductionOrderId());
        record.setPhase(phase);
        record.setWasteCategory(MERMA_OPERATIVA);
        record.setWasteOrigin(WasteMakeready.ORIGIN_EXCESO);
        record.setMaterialType(materialType);
        record.setPaperRowId(null);
        record.setPlannedQuantity(WasteValuation.plannedQuantity(units, percentage));
        record.setUnitCostSnapshot(stepUnitCost(order, phase));
        record.setUpdatedAt(now);
        WasteRecord saved = wasteRecordRepository.save(record);
        List<String> extras = existing.stream()
                .map(WasteRecord::getWasteRecordId)
                .filter(id -> !id.equals(saved.getWasteRecordId()))
                .toList();
        wasteRecordRepository.deleteByIds(extras);
    }

    private static BigDecimal sumEstimatedMachineCost(ProductionOrder order, String phase) {
        BigDecimal total = BigDecimal.ZERO;
        if (order.getMachineUsages() == null) {
            return WasteValuation.money(total);
        }
        for (MachineUsage usage : order.getMachineUsages()) {
            if (phase.equals(usage.getPhase())) {
                total = total.add(WasteValuation.money(usage.getEstimatedMachineCost()));
            }
        }
        return WasteValuation.money(total);
    }

    private static BigDecimal sumPlannedWasteCost(ProductionOrder order, String phase) {
        BigDecimal total = BigDecimal.ZERO;
        if (order.getWasteRecords() == null) {
            return WasteValuation.money(total);
        }
        for (WasteRecord record : order.getWasteRecords()) {
            if (phase.equals(record.getPhase())) {
                total = total.add(WasteValuation.money(record.getPlannedCost()));
            }
        }
        return WasteValuation.money(total);
    }

    private static int stepUnits(ProductionOrder order, String phase) {
        if (PHASE_PREPRESS.equals(phase)) {
            int plates = 0;
            if (order.getPlates() != null) {
                for (Plate plate : order.getPlates()) {
                    if (plate.getPlatesCount() != null && plate.getPlatesCount() > 0) {
                        plates += plate.getPlatesCount();
                    }
                }
            }
            return plates;
        }
        PostpressType type = PHASE_FINISHED.equals(phase)
                ? PostpressType.FINISHED_PRODUCT
                : PostpressType.FINISHING_PROCESS;
        int pieces = 0;
        if (order.getPostpressRecords() != null) {
            for (PostpressRecord record : order.getPostpressRecords()) {
                if (record.getType() != type || record.getLines() == null) {
                    continue;
                }
                for (PostpressLine line : record.getLines()) {
                    if (line.getGoodSizes() != null && line.getGoodSizes() > 0) {
                        pieces += line.getGoodSizes();
                    }
                }
            }
        }
        return pieces;
    }

    private static BigDecimal stepUnitCost(ProductionOrder order, String phase) {
        if (PHASE_PREPRESS.equals(phase)) {
            if (order.getPrepress() != null && Boolean.TRUE.equals(order.getPrepress().getClientSuppliesPlates())) {
                return WasteValuation.money(BigDecimal.ZERO);
            }
            BigDecimal weighted = BigDecimal.ZERO;
            int plates = 0;
            if (order.getPlates() != null) {
                for (Plate plate : order.getPlates()) {
                    int count = plate.getPlatesCount() == null ? 0 : plate.getPlatesCount();
                    if (count <= 0) {
                        continue;
                    }
                    plates += count;
                    weighted = weighted.add(WasteValuation.money(plate.getPlatePrice()).multiply(BigDecimal.valueOf(count)));
                }
            }
            if (plates == 0) {
                return WasteValuation.money(BigDecimal.ZERO);
            }
            return weighted.divide(BigDecimal.valueOf(plates), 2, RoundingMode.HALF_UP);
        }
        PostpressType type = PHASE_FINISHED.equals(phase)
                ? PostpressType.FINISHED_PRODUCT
                : PostpressType.FINISHING_PROCESS;
        BigDecimal charged = BigDecimal.ZERO;
        int pieces = 0;
        if (order.getPostpressRecords() != null) {
            for (PostpressRecord record : order.getPostpressRecords()) {
                if (record.getType() != type || record.getLines() == null) {
                    continue;
                }
                for (PostpressLine line : record.getLines()) {
                    int count = line.getGoodSizes() == null ? 0 : line.getGoodSizes();
                    if (count <= 0) {
                        continue;
                    }
                    pieces += count;
                    charged = charged.add(WasteValuation.money(line.getChargedPrice()));
                }
            }
        }
        if (pieces == 0) {
            return WasteValuation.money(BigDecimal.ZERO);
        }
        return charged.divide(BigDecimal.valueOf(pieces), 2, RoundingMode.HALF_UP);
    }

    private void applyActualMinutes(
            ProductionOrder order,
            String phase,
            Integer actualSetupMinutes,
            Integer actualRunMinutes,
            String machineId
    ) {
        if (actualSetupMinutes == null && actualRunMinutes == null) {
            return;
        }
        List<MachineUsage> usages = machineUsageRepository.findByProductionOrderIdAndPhase(
                order.getCompanyId(), order.getProductionOrderId(), phase);
        if (machineId != null && !machineId.isBlank()) {
            usages = usages.stream().filter(usage -> machineId.trim().equals(usage.getMachineId())).toList();
            if (usages.isEmpty()) {
                throw new ResourceNotFoundException("MACHINE_USAGE_NOT_FOUND", "La fase no tiene esa máquina cotizada");
            }
        } else if (usages.size() > 1) {
            throw new ProductionOrderBusinessRuleException(
                    "La fase tiene varias máquinas; indique machineId para registrar los minutos reales");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (MachineUsage usage : usages) {
            if (actualSetupMinutes != null) {
                usage.setActualSetupMinutes(nonNegativeMinutes(actualSetupMinutes, "actualSetupMinutes"));
            }
            if (actualRunMinutes != null) {
                usage.setActualRunMinutes(nonNegativeMinutes(actualRunMinutes, "actualRunMinutes"));
            }
            usage.setUpdatedAt(now);
            machineUsageRepository.save(usage);
        }
    }

    private void applyActualWaste(ProductionOrder order, String phase, BigDecimal observed, String note) {
        String plannedCategory = plannedCategoryFor(phase);
        List<WasteRecord> planned = plannedCategory == null
                ? List.of()
                : wasteRecordRepository.findByProductionOrderPhaseAndCategory(
                order.getCompanyId(), order.getProductionOrderId(), phase, plannedCategory);
        WasteValuation.Allocation allocation = WasteValuation.allocate(
                planned.stream().map(WasteRecord::getPlannedQuantity).toList(),
                observed
        );
        LocalDateTime now = LocalDateTime.now(clock);
        for (int i = 0; i < planned.size(); i++) {
            WasteRecord record = planned.get(i);
            record.setActualQuantity(allocation.assignedToPlanned().get(i));
            record.setNote(WasteMakeready.attachReason(record.getNote(), note));
            record.setUpdatedAt(now);
            wasteRecordRepository.save(record);
        }

        List<WasteRecord> previousExcess = wasteRecordRepository.findByProductionOrderPhaseAndCategory(
                order.getCompanyId(), order.getProductionOrderId(), phase, DESPERDICIO);
        List<String> excesoIds = previousExcess.stream()
                .filter(record -> !WasteMakeready.ORIGIN_RETRABAJO.equals(record.getWasteOrigin()))
                .map(WasteRecord::getWasteRecordId)
                .toList();
        wasteRecordRepository.deleteByIds(excesoIds);

        if (allocation.excess().signum() <= 0) {
            return;
        }
        WasteRecord excess = new WasteRecord();
        excess.setCompanyId(order.getCompanyId());
        excess.setProductionOrderId(order.getProductionOrderId());
        excess.setPhase(phase);
        excess.setWasteCategory(DESPERDICIO);
        excess.setWasteOrigin(WasteMakeready.ORIGIN_EXCESO);
        excess.setMaterialType(materialTypeFor(phase));
        excess.setPlannedQuantity(WasteValuation.money(BigDecimal.ZERO));
        excess.setActualQuantity(allocation.excess());
        excess.setUnitCostSnapshot(excessUnitCost(order, phase, planned));
        excess.setNote(note);
        excess.setCreatedAt(now);
        excess.setUpdatedAt(now);
        wasteRecordRepository.save(excess);
    }

    private BigDecimal excessUnitCost(ProductionOrder order, String phase) {
        String category = plannedCategoryFor(phase);
        List<WasteRecord> planned = category == null
                ? List.of()
                : wasteRecordRepository.findByProductionOrderPhaseAndCategory(
                order.getCompanyId(), order.getProductionOrderId(), phase, category);
        return excessUnitCost(order, phase, planned);
    }

    private BigDecimal excessUnitCost(ProductionOrder order, String phase, List<WasteRecord> planned) {
        List<MachineUsage> usages = machineUsageRepository.findByProductionOrderIdAndPhase(
                order.getCompanyId(), order.getProductionOrderId(), phase);
        BigDecimal machineCost = BigDecimal.ZERO;
        for (MachineUsage usage : usages) {
            BigDecimal cost = usage.getActualMachineCost() != null
                    ? usage.getActualMachineCost()
                    : usage.getEstimatedMachineCost();
            machineCost = machineCost.add(WasteValuation.money(cost));
        }
        return WasteValuation.proratedUnitCost(
                weightedMaterialUnit(planned),
                machineCost,
                inkCost(order, phase),
                prorateBase(order, phase)
        );
    }

    private void assertMachineOnPhase(ProductionOrder order, String phase, String machineId) {
        if (machineId == null || machineId.isBlank()) {
            return;
        }
        boolean found = machineUsageRepository.findByProductionOrderIdAndPhase(
                        order.getCompanyId(), order.getProductionOrderId(), phase)
                .stream()
                .anyMatch(usage -> machineId.trim().equals(usage.getMachineId()));
        if (!found) {
            throw new ResourceNotFoundException("MACHINE_USAGE_NOT_FOUND", "La fase no tiene esa máquina cotizada");
        }
    }

    private static BigDecimal resolveMakeready(BigDecimal requested, BigDecimal companyDefault) {
        if (requested == null) {
            return WasteValuation.money(companyDefault);
        }
        if (requested.signum() < 0) {
            throw new ProductionOrderBusinessRuleException("Los pliegos fijos de arranque no pueden ser negativos");
        }
        return WasteValuation.money(requested);
    }

    private static BigDecimal prorateBase(ProductionOrder order, String phase) {
        if (PHASE_CUTTING.equals(phase) || PHASE_PRINTING.equals(phase)) {
            return baseQuantity(order);
        }
        int units = stepUnits(order, phase);
        return units > 0 ? BigDecimal.valueOf(units) : BigDecimal.ONE;
    }

    private static String plannedCategoryFor(String phase) {
        if (PHASE_CUTTING.equals(phase)) {
            return MERMA_CORTE;
        }
        if (PHASE_PREPRESS.equals(phase) || PHASE_PRINTING.equals(phase)
                || PHASE_FINISHED.equals(phase) || PHASE_FINISHING.equals(phase)) {
            return MERMA_OPERATIVA;
        }
        return null;
    }

    private static String materialTypeFor(String phase) {
        if (PHASE_CUTTING.equals(phase) || PHASE_PRINTING.equals(phase)) {
            return "papel";
        }
        if (PHASE_PREPRESS.equals(phase)) {
            return "plancha";
        }
        if (PHASE_FINISHED.equals(phase) || PHASE_FINISHING.equals(phase)) {
            return "acabado";
        }
        return "otro";
    }

    private static BigDecimal weightedMaterialUnit(List<WasteRecord> planned) {
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal weighted = BigDecimal.ZERO;
        for (WasteRecord record : planned) {
            BigDecimal quantity = WasteValuation.money(record.getPlannedQuantity());
            weight = weight.add(quantity);
            weighted = weighted.add(quantity.multiply(WasteValuation.money(record.getUnitCostSnapshot())));
        }
        if (weight.signum() <= 0) {
            return planned.isEmpty()
                    ? WasteValuation.money(BigDecimal.ZERO)
                    : WasteValuation.money(planned.get(0).getUnitCostSnapshot());
        }
        return weighted.divide(weight, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal inkCost(ProductionOrder order, String phase) {
        if (!PHASE_PRINTING.equals(phase) || order.getPrints() == null) {
            return WasteValuation.money(BigDecimal.ZERO);
        }
        BigDecimal ink = BigDecimal.ZERO;
        for (PrintConfig print : order.getPrints()) {
            if (print.getEntries() == null) {
                continue;
            }
            for (PrintEntry entry : print.getEntries()) {
                if (!Boolean.TRUE.equals(entry.getClientSuppliesPantoneInk())) {
                    ink = ink.add(WasteValuation.money(entry.getPantoneInkChargePrice()));
                }
            }
        }
        return ink;
    }

    private static BigDecimal baseQuantity(ProductionOrder order) {
        int sheets = totalSheets(order);
        if (sheets > 0) {
            return BigDecimal.valueOf(sheets);
        }
        return BigDecimal.valueOf(Math.max(order.getRequestedQuantity(), 1));
    }

    private static int totalSheets(ProductionOrder order) {
        int sheets = 0;
        if (order.getPaperRows() == null) {
            return 0;
        }
        for (PaperRow row : order.getPaperRows()) {
            if (row.getCalculatedSheetsCount() != null && row.getCalculatedSheetsCount() > 0) {
                sheets += row.getCalculatedSheetsCount();
            }
        }
        return sheets;
    }

    private static BigDecimal averageSheetValue(ProductionOrder order) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        if (order.getPaperRows() != null) {
            for (PaperRow row : order.getPaperRows()) {
                if (row.isClientSuppliesPaper() || row.getSheetValue() == null) {
                    continue;
                }
                sum = sum.add(row.getSheetValue());
                count++;
            }
        }
        if (count == 0) {
            return WasteValuation.money(BigDecimal.ZERO);
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static int nonNegativeMinutes(Integer minutes, String field) {
        int value = minutes == null ? 0 : minutes;
        if (value < 0) {
            throw new ProductionOrderBusinessRuleException(field + " no puede ser negativo");
        }
        return value;
    }

    private static boolean isMachinePhase(String phase) {
        try {
            MachineType.fromApiValue(phase);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
