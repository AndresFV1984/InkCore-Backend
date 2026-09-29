package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.machine.ports.out.MachineRepositoryPort;
import com.inkcore.domain.productionorder.model.CostSummary;
import com.inkcore.domain.productionorder.model.MachineUsage;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProfitabilityRow;
import com.inkcore.domain.productionorder.model.WasteRecord;
import com.inkcore.domain.productionorder.ports.out.CostSummaryRepositoryPort;
import com.inkcore.domain.productionorder.ports.out.MachineUsageRepositoryPort;
import com.inkcore.domain.productionorder.ports.out.WasteRecordRepositoryPort;
import com.inkcore.domain.wastesettings.model.CompanyWasteSettings;
import com.inkcore.domain.wastesettings.ports.out.CompanyWasteSettingsRepositoryPort;
import com.inkcore.infrastructure.in.rest.productionorders.CostSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionOrderCostingCoordinatorTest {

    private MemoryWaste waste;
    private MemoryCostSummary costs;
    private ProductionOrderCostingCoordinator coordinator;
    private ProductionOrder order;

    @BeforeEach
    void setUp() {
        waste = new MemoryWaste();
        costs = new MemoryCostSummary();
        coordinator = new ProductionOrderCostingCoordinator(
                new MemoryMachines(),
                new MemoryUsages(),
                waste,
                new MemorySettings(),
                costs,
                Clock.fixed(Instant.parse("2026-09-25T17:00:00Z"), ZoneOffset.UTC)
        );
        order = ProductionOrder.createNew(
                "company-1",
                "OP-1",
                "client-1",
                "Trabajo",
                null,
                LocalDate.of(2026, 9, 25),
                1000,
                null,
                null,
                "user-1",
                LocalDateTime.of(2026, 9, 25, 12, 0)
        );
    }

    @Test
    void cuttingWithinPlanDoesNotCreateDesperdicioAndExcessDoes() {
        seedMerma("corte-papel", "merma_corte", "20.00");

        coordinator.capturePhaseClose(order, "corte-papel", new BigDecimal("10"), null, null, null,
                "mal_cortado");

        assertEquals(0, desperdicios("corte-papel").size());
        assertEquals(0, new BigDecimal("10.00").compareTo(planned("corte-papel", "merma_corte").getActualQuantity()));
        assertEquals(0, costs.quotedWrites);
    }

    @Test
    void printingExcessIsReplacedOnTheNextClose() {
        seedMerma("impresion", "merma_operativa", "20.00");

        coordinator.capturePhaseClose(order, "impresion", new BigDecimal("30"), null, null, null,
                "defecto_impresion");
        List<WasteRecord> first = desperdicios("impresion");
        assertEquals(1, first.size());
        assertEquals("exceso", first.get(0).getWasteOrigin());
        assertEquals(0, new BigDecimal("10.00").compareTo(first.get(0).getActualQuantity()));
        assertEquals("Defecto de impresión", first.get(0).getNote());
        String firstId = first.get(0).getWasteRecordId();

        coordinator.capturePhaseClose(order, "impresion", new BigDecimal("25"), null, null, null,
                "otro");
        List<WasteRecord> second = desperdicios("impresion");
        assertEquals(1, second.size());
        assertEquals("exceso", second.get(0).getWasteOrigin());
        assertEquals(0, new BigDecimal("5.00").compareTo(second.get(0).getActualQuantity()));
        assertTrue(second.stream().noneMatch(record -> firstId.equals(record.getWasteRecordId())));
        assertEquals(0, costs.quotedWrites);
    }

    @Test
    void stepPhasesKeepExcessOutOfOperationalMerma() {
        for (String phase : List.of("preprensa", "terminados", "acabados")) {
            seedMerma(phase, "merma_operativa", "2.00");
            coordinator.capturePhaseClose(order, phase, new BigDecimal("5"), null, null, null,
                    "defecto_impresion");
            WasteRecord merma = planned(phase, "merma_operativa");
            assertEquals(0, new BigDecimal("2.00").compareTo(merma.getActualQuantity()));
            List<WasteRecord> excess = desperdicios(phase);
            assertEquals(1, excess.size());
            assertEquals("exceso", excess.get(0).getWasteOrigin());
            assertEquals(0, new BigDecimal("3.00").compareTo(excess.get(0).getActualQuantity()));
        }
    }

    @Test
    void reprintSurvivesALaterPhaseCloseAndDoesNotChangeQuotedPrice() {
        seedMerma("impresion", "merma_operativa", "20.00");
        WasteRecord reprint = coordinator.registerReprint(
                order, "impresion", new BigDecimal("8"), "cambio_medio_tiro", null);
        assertEquals("retrabajo", reprint.getWasteOrigin());
        assertEquals("desperdicio", reprint.getWasteCategory());
        assertEquals(0, BigDecimal.ZERO.compareTo(reprint.getPlannedQuantity()));
        assertEquals(0, costs.quotedWrites);

        coordinator.capturePhaseClose(order, "impresion", new BigDecimal("30"), null, null, null,
                "defecto_impresion");

        List<WasteRecord> rows = desperdicios("impresion");
        assertEquals(1, rows.stream().filter(record -> "retrabajo".equals(record.getWasteOrigin())).count());
        assertEquals(1, rows.stream().filter(record -> "exceso".equals(record.getWasteOrigin())).count());

        coordinator.capturePhaseClose(order, "impresion", new BigDecimal("10"), null, null, null,
                "otro");
        List<WasteRecord> afterFit = desperdicios("impresion");
        assertEquals(1, afterFit.size());
        assertEquals("retrabajo", afterFit.get(0).getWasteOrigin());
        assertEquals(0, costs.quotedWrites);
    }

    @Test
    void cuttingExcessIsReplacedAndObservedQuantityWithoutPlanIsAllDesperdicio() {
        seedMerma("corte-papel", "merma_corte", "20.00");
        coordinator.capturePhaseClose(order, "corte-papel", new BigDecimal("28"), null, null, null,
                "mal_cortado");
        assertEquals(1, desperdicios("corte-papel").size());
        assertEquals(0, new BigDecimal("8.00").compareTo(desperdicios("corte-papel").get(0).getActualQuantity()));

        coordinator.capturePhaseClose(order, "corte-papel", new BigDecimal("22"), null, null, null,
                "papel_mala_calidad");
        assertEquals(1, desperdicios("corte-papel").size());
        assertEquals(0, new BigDecimal("2.00").compareTo(desperdicios("corte-papel").get(0).getActualQuantity()));
        assertEquals("Papel de mala calidad", desperdicios("corte-papel").get(0).getNote());

        coordinator.capturePhaseClose(order, "acabados", new BigDecimal("7"), null, null, null, "otro");
        assertTrue(waste.findByProductionOrderPhaseAndCategory(
                order.getCompanyId(), order.getProductionOrderId(), "acabados", "merma_operativa").isEmpty());
        List<WasteRecord> excess = desperdicios("acabados");
        assertEquals(1, excess.size());
        assertEquals("exceso", excess.get(0).getWasteOrigin());
        assertEquals(0, new BigDecimal("7.00").compareTo(excess.get(0).getActualQuantity()));
        assertEquals(0, costs.quotedWrites);
    }

    @Test
    void fixedSheetsZeroMatchesPercentageOnlyAndFixedSheetsAreAddedOnce() {
        PaperRow row = new PaperRow();
        row.setPaperRowId("row-1");
        row.setCalculatedSheetsCount(1000);
        row.setPlannedWastePercentage(new BigDecimal("2"));
        row.setSheetValue(new BigDecimal("100"));
        order.setPaperRows(List.of(row));

        coordinator.afterPrinting(order, null, new BigDecimal("2"), BigDecimal.ZERO);
        assertEquals(0, new BigDecimal("20.00").compareTo(planned("impresion", "merma_operativa").getPlannedQuantity()));

        coordinator.afterPrinting(order, null, new BigDecimal("2"), new BigDecimal("400"));
        assertEquals(0, new BigDecimal("420.00").compareTo(planned("impresion", "merma_operativa").getPlannedQuantity()));

        coordinator.afterCutting(order, null, new BigDecimal("400"));
        assertEquals(0, new BigDecimal("420.00").compareTo(planned("corte-papel", "merma_corte").getPlannedQuantity()));
    }

    @Test
    void costSummaryExposesMermaAndDesperdicioWithoutRecomputingTheTotal() {
        CostSummary summary = new CostSummary(
                "op-1",
                "company-1",
                new BigDecimal("800000.00"),
                new BigDecimal("600000.00"),
                new BigDecimal("96000.00"),
                new BigDecimal("48000.00"),
                new BigDecimal("1496000.00"),
                new BigDecimal("800000.00"),
                new BigDecimal("720000.00"),
                new BigDecimal("96000.00"),
                new BigDecimal("52000.00"),
                new BigDecimal("44000.00"),
                new BigDecimal("1616000.00"),
                new BigDecimal("1650000.00"),
                new BigDecimal("154000.00"),
                new BigDecimal("34000.00"),
                new BigDecimal("2.06"),
                LocalDateTime.of(2026, 9, 25, 12, 0)
        );
        WasteRecord desperdicio = new WasteRecord();
        desperdicio.setPhase("impresion");
        desperdicio.setWasteCategory("desperdicio");
        desperdicio.setWasteOrigin("exceso");
        desperdicio.setActualQuantity(new BigDecimal("12.00"));
        desperdicio.setActualCost(new BigDecimal("44000.00"));
        desperdicio.setNote("Defecto de impresión");

        CostSummaryResponse response = CostSummaryResponse.from(
                new GetProductionOrderCostSummaryUseCase.ProductionOrderCostRead(summary, List.of(desperdicio)));

        assertEquals(0, new BigDecimal("48000.00").compareTo(response.estimatedMermaCost()));
        assertEquals(0, new BigDecimal("52000.00").compareTo(response.actualMermaCost()));
        assertEquals(0, new BigDecimal("44000.00").compareTo(response.actualDesperdicioCost()));
        assertEquals(0, new BigDecimal("96000.00").compareTo(response.actualWasteCost()));
        assertEquals(0, new BigDecimal("1616000.00").compareTo(response.actualTotalCost()));
        assertEquals(0, new BigDecimal("1650000.00").compareTo(response.quotedPrice()));
        assertEquals(1, response.desperdicios().size());
        assertEquals("exceso", response.desperdicios().get(0).wasteOrigin());
    }

    private void seedMerma(String phase, String category, String planned) {
        WasteRecord record = new WasteRecord();
        record.setCompanyId(order.getCompanyId());
        record.setProductionOrderId(order.getProductionOrderId());
        record.setPhase(phase);
        record.setWasteCategory(category);
        record.setMaterialType("papel");
        record.setPlannedQuantity(new BigDecimal(planned));
        record.setUnitCostSnapshot(new BigDecimal("100.00"));
        waste.save(record);
    }

    private WasteRecord planned(String phase, String category) {
        return waste.findByProductionOrderPhaseAndCategory(
                order.getCompanyId(), order.getProductionOrderId(), phase, category).get(0);
    }

    private List<WasteRecord> desperdicios(String phase) {
        return waste.findByProductionOrderPhaseAndCategory(
                order.getCompanyId(), order.getProductionOrderId(), phase, "desperdicio");
    }

    private static final class MemoryWaste implements WasteRecordRepositoryPort {
        private final List<WasteRecord> rows = new ArrayList<>();

        @Override
        public List<WasteRecord> findByProductionOrderId(String companyId, String productionOrderId) {
            return rows.stream()
                    .filter(row -> companyId.equals(row.getCompanyId()) && productionOrderId.equals(row.getProductionOrderId()))
                    .toList();
        }

        @Override
        public List<WasteRecord> findByProductionOrderPhaseAndCategory(
                String companyId, String productionOrderId, String phase, String wasteCategory) {
            return findByProductionOrderId(companyId, productionOrderId).stream()
                    .filter(row -> phase.equals(row.getPhase()) && wasteCategory.equals(row.getWasteCategory()))
                    .toList();
        }

        @Override
        public void deleteByProductionOrderId(String productionOrderId) {
            rows.removeIf(row -> productionOrderId.equals(row.getProductionOrderId()));
        }

        @Override
        public void deleteByIds(List<String> wasteRecordIds) {
            if (wasteRecordIds == null || wasteRecordIds.isEmpty()) {
                return;
            }
            rows.removeIf(row -> wasteRecordIds.contains(row.getWasteRecordId()));
        }

        @Override
        public WasteRecord save(WasteRecord record) {
            record.setPlannedCost(record.getPlannedQuantity().multiply(record.getUnitCostSnapshot()));
            record.setActualCost(record.getActualQuantity() == null
                    ? null
                    : record.getActualQuantity().multiply(record.getUnitCostSnapshot()));
            rows.removeIf(row -> row.getWasteRecordId().equals(record.getWasteRecordId()));
            rows.add(record);
            return record;
        }
    }

    private static final class MemoryUsages implements MachineUsageRepositoryPort {
        @Override
        public List<MachineUsage> findByProductionOrderId(String companyId, String productionOrderId) {
            return List.of();
        }

        @Override
        public List<MachineUsage> findByProductionOrderIdAndPhase(String companyId, String productionOrderId, String phase) {
            return List.of();
        }

        @Override
        public void deleteByProductionOrderIdAndPhase(String productionOrderId, String phase) {
        }

        @Override
        public void deleteByProductionOrderIdAndPhaseNot(String productionOrderId, String phase) {
        }

        @Override
        public MachineUsage save(MachineUsage usage) {
            return usage;
        }

        @Override
        public List<MachineUsage> replacePhase(String productionOrderId, String phase, List<MachineUsage> usages) {
            return usages;
        }
    }

    private static final class MemoryMachines implements MachineRepositoryPort {
        @Override
        public com.inkcore.domain.machine.model.Machine save(com.inkcore.domain.machine.model.Machine machine, String changedBy) {
            return machine;
        }

        @Override
        public Optional<com.inkcore.domain.machine.model.Machine> findById(String machineId) {
            return Optional.empty();
        }

        @Override
        public com.inkcore.domain.shared.PageResult<com.inkcore.domain.machine.model.Machine> findPage(
                String companyId, Boolean state, String machineType, com.inkcore.domain.shared.PageQuery pageQuery) {
            return null;
        }

        @Override
        public boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name) {
            return false;
        }

        @Override
        public boolean existsByCompanyIdAndNameIgnoreCaseExcludingId(String companyId, String name, String machineId) {
            return false;
        }

        @Override
        public int snapshotActiveRates(String companyId, String changedBy) {
            return 0;
        }

        @Override
        public List<String> findCompanyIdsWithActiveMachines() {
            return List.of();
        }

        @Override
        public List<com.inkcore.domain.machine.model.Machine> findActiveByCompanyId(String companyId) {
            return List.of();
        }
    }

    private static final class MemorySettings implements CompanyWasteSettingsRepositoryPort {
        @Override
        public Optional<CompanyWasteSettings> findByCompanyId(String companyId) {
            return Optional.of(CompanyWasteSettings.initialSuggestion(companyId));
        }

        @Override
        public CompanyWasteSettings save(CompanyWasteSettings settings) {
            return settings;
        }
    }

    private static final class MemoryCostSummary implements CostSummaryRepositoryPort {
        private int quotedWrites;

        @Override
        public Optional<CostSummary> findByProductionOrderId(String companyId, String productionOrderId) {
            return Optional.empty();
        }

        @Override
        public void upsertQuotedPrice(String productionOrderId, String companyId, BigDecimal quotedPrice) {
            quotedWrites++;
        }

        @Override
        public List<ProfitabilityRow> findProfitability(
                String companyId, LocalDate from, LocalDate to, String clientId, String sellerId) {
            return List.of();
        }
    }
}
