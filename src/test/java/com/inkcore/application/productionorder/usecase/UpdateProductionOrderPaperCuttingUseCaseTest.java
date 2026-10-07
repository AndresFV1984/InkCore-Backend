package com.inkcore.application.productionorder.usecase;

import com.inkcore.application.paper.usecase.PaperPricingSettings;
import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.paper.model.Paper;
import com.inkcore.domain.paper.model.PaperCutLayout;
import com.inkcore.domain.paper.model.PaperSupplierPrice;
import com.inkcore.domain.paper.model.PriceRule;
import com.inkcore.domain.paper.ports.out.PaperCutLayoutRepositoryPort;
import com.inkcore.domain.paper.ports.out.PaperRepositoryPort;
import com.inkcore.domain.paper.ports.out.PaperSupplierPriceRepositoryPort;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import com.inkcore.domain.wastesettings.model.CompanyWasteSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductionOrderPaperCuttingUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-22T12:00:00Z");

    @Mock ProductionOrderRepositoryPort repository;
    @Mock AuthenticatedCompanyResolver companyResolver;
    @Mock PaperRepositoryPort paperRepository;
    @Mock PaperSupplierPriceRepositoryPort paperPriceRepository;
    @Mock PaperCutLayoutRepositoryPort paperCutLayoutRepository;
    @Mock CutLayoutRepositoryPort cutLayoutRepository;
    @Mock UserRepositoryPort userRepository;
    @Mock ProductionOrderCostingCoordinator costing;
    @Mock PaperRemnantStockSync remnantStockSync;

    private UpdateProductionOrderPaperCuttingUseCase useCase;

    @BeforeEach
    void setUp() {
        ProductionOrderSupport support = new ProductionOrderSupport(
                repository,
                companyResolver,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
        useCase = new UpdateProductionOrderPaperCuttingUseCase(
                support,
                new ProductionOrderOperatorsApplier(userRepository),
                paperRepository,
                paperPriceRepository,
                paperCutLayoutRepository,
                cutLayoutRepository,
                new PaperPricingSettings(30, new BigDecimal("19"), true),
                costing,
                remnantStockSync
        );
    }

    @Test
    void execute_persistsCatalogSnapshotsEvenWhenClientSuppliesCutPaper() {
        var auth = new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-1");
        when(companyResolver.resolveUserId(auth)).thenReturn("user-1");
        when(costing.resolveCutPercentage(eq("company-1"), any(), anyBoolean()))
                .thenReturn(BigDecimal.ZERO);
        when(costing.afterCutting(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));

        ProductionOrder order = baseOrder();
        when(repository.findById("order-1")).thenReturn(Optional.of(order));
        when(repository.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        stubPaperCatalog("papel-1", "Bond", false, "sup-1", new BigDecimal("120"), true);
        when(paperCutLayoutRepository.findByPaperId("company-1", "papel-1")).thenReturn(List.of(
                paperCut("pcl-1", "papel-1", "corte-1")
        ));
        when(cutLayoutRepository.findById("corte-1")).thenReturn(Optional.of(cutLayout("corte-1", "2x2", 4)));

        useCase.execute(
                "order-1",
                command(List.of(new UpdateProductionOrderPaperCuttingCommand.PaperRowInput(
                        null, "plate-srv-1", null, "plate-srv-1",
                        false, null, true,
                        "papel-1", "sup-1", "corte-1", null, null,
                        true, 100, null, null, null, null
                ))),
                auth
        );

        ArgumentCaptor<ProductionOrder> captor = ArgumentCaptor.forClass(ProductionOrder.class);
        verify(repository).save(captor.capture());
        PaperRow saved = captor.getValue().getPaperRows().get(0);
        assertEquals("plate-srv-1", saved.getPlateId());
        assertEquals("papel-1", saved.getPaperId());
        assertEquals("sup-1", saved.getSupplierId());
        assertEquals(0, new BigDecimal("120").compareTo(saved.getSheetValue()));
        assertEquals(500, saved.getPackageUnit());
        assertEquals("Bond", saved.getPaperName());
        assertEquals("70x100cm", saved.getPaperSize());
        assertEquals("corte-1", saved.getCutLayoutId());
        assertEquals("2x2", saved.getCutLayoutName());
        assertEquals(4, saved.getPiecesPerSheet());
        assertEquals(0, new BigDecimal("50.00").compareTo(saved.getCutValue()));
        assertEquals(Boolean.FALSE, saved.getCoatedSnapshot());
        assertNotNull(saved.getFreightPerSheetSnapshot());
        assertEquals(PriceRule.PREFERRED, saved.getPriceRule());
    }

    @Test
    void execute_usesPreferredPriceWhenSupplierIdMissing() {
        var auth = new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-1");
        when(companyResolver.resolveUserId(auth)).thenReturn("user-1");
        when(costing.resolveCutPercentage(eq("company-1"), any(), anyBoolean()))
                .thenReturn(new BigDecimal("2.00"));
        when(costing.settingsFor("company-1")).thenReturn(CompanyWasteSettings.initialSuggestion("company-1"));
        when(costing.afterCutting(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));

        ProductionOrder order = baseOrder();
        when(repository.findById("order-1")).thenReturn(Optional.of(order));
        when(repository.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        Paper paper = Paper.reconstitute(
                "papel-1", "company-1", "Bond", new BigDecimal("75"),
                new BigDecimal("70"), new BigDecimal("100"), "cm",
                false, false, null, null, null, true,
                LocalDate.of(2026, 1, 1), LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        when(paperRepository.findById("papel-1")).thenReturn(Optional.of(paper));
        when(paperPriceRepository.findByPaperId("company-1", "papel-1")).thenReturn(List.of(
                price("p-low", "papel-1", "sup-low", new BigDecimal("80"), false),
                price("p-pref", "papel-1", "sup-pref", new BigDecimal("150"), true)
        ));
        when(paperCutLayoutRepository.findByPaperId("company-1", "papel-1")).thenReturn(List.of(
                paperCut("pcl-1", "papel-1", "corte-1")
        ));
        when(cutLayoutRepository.findById("corte-1")).thenReturn(Optional.of(cutLayout("corte-1", "2x2", 4)));

        useCase.execute(
                "order-1",
                command(List.of(new UpdateProductionOrderPaperCuttingCommand.PaperRowInput(
                        null, "plate-srv-1", null, "plate-srv-1",
                        false, null, false,
                        "papel-1", null, "corte-1", null, null,
                        null, null, null, null, null, null
                ))),
                auth
        );

        ArgumentCaptor<ProductionOrder> captor = ArgumentCaptor.forClass(ProductionOrder.class);
        verify(repository).save(captor.capture());
        PaperRow saved = captor.getValue().getPaperRows().get(0);
        assertEquals("sup-pref", saved.getSupplierId());
        assertEquals(0, new BigDecimal("150").compareTo(saved.getSheetValue()));
        assertEquals(PriceRule.PREFERRED, saved.getPriceRule());
        assertNotNull(saved.getCalculatedSheetsCount());
        assertNotNull(saved.getTotalPaperValue());
        assertNotNull(saved.getTotalCutValue());
    }

    @Test
    void execute_selectsBestCostSupplierWhenPriceRuleProvided() {
        var auth = new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-1");
        when(companyResolver.resolveUserId(auth)).thenReturn("user-1");
        when(costing.resolveCutPercentage(eq("company-1"), any(), anyBoolean()))
                .thenReturn(new BigDecimal("2.00"));
        when(costing.settingsFor("company-1")).thenReturn(CompanyWasteSettings.initialSuggestion("company-1"));
        when(costing.afterCutting(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));

        ProductionOrder order = baseOrder();
        when(repository.findById("order-1")).thenReturn(Optional.of(order));
        when(repository.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        stubPaperCatalog("papel-1", "Bond", true, "sup-expensive", new BigDecimal("200"), true);
        when(paperPriceRepository.findByPaperId("company-1", "papel-1")).thenReturn(List.of(
                price("p-exp", "papel-1", "sup-expensive", new BigDecimal("200"), true),
                price("p-cheap", "papel-1", "sup-cheap", new BigDecimal("100"), false)
        ));
        when(paperCutLayoutRepository.findById("pcl-1")).thenReturn(Optional.of(
                paperCut("pcl-1", "papel-1", "corte-1")
        ));
        when(cutLayoutRepository.findById("corte-1")).thenReturn(Optional.of(cutLayout("corte-1", "2x2", 4)));

        useCase.execute(
                "order-1",
                command(List.of(new UpdateProductionOrderPaperCuttingCommand.PaperRowInput(
                        null, "plate-srv-1", null, "plate-srv-1",
                        false, null, false,
                        "papel-1", null, null, "pcl-1", "BEST_COST",
                        null, null, null, null, new BigDecimal("2.00"), null
                ))),
                auth
        );

        ArgumentCaptor<ProductionOrder> captor = ArgumentCaptor.forClass(ProductionOrder.class);
        verify(repository).save(captor.capture());
        PaperRow saved = captor.getValue().getPaperRows().get(0);
        assertEquals("sup-cheap", saved.getSupplierId());
        assertEquals(PriceRule.BEST_COST, saved.getPriceRule());
        assertEquals("corte-1", saved.getCutLayoutId());
        assertEquals(4, saved.getPiecesPerSheetSnapshot());
        assertEquals(Boolean.TRUE, saved.getCoatedSnapshot());
        assertTrue(saved.getNetSheets().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(saved.getTotalSheets());
        assertNotNull(saved.getCostPerPiece());
        assertTrue(saved.getTotalPaperValue().compareTo(BigDecimal.ZERO) > 0);
    }

    private static ProductionOrder baseOrder() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("order-1");
        order.setCompanyId("company-1");
        order.setVersion(2L);
        Plate plate = new Plate();
        plate.setPlateId("plate-srv-1");
        plate.setQuantity(1000);
        plate.setCavities(4);
        plate.setGoodSizes(250);
        order.setPlates(List.of(plate));
        return order;
    }

    private static UpdateProductionOrderPaperCuttingCommand command(
            List<UpdateProductionOrderPaperCuttingCommand.PaperRowInput> rows
    ) {
        return new UpdateProductionOrderPaperCuttingCommand(
                2L, false, 2, true, null, null, "%", BigDecimal.ZERO, rows, null, null
        );
    }

    private void stubPaperCatalog(
            String paperId,
            String paperName,
            boolean coated,
            String supplierId,
            BigDecimal sheetValue,
            boolean preferred
    ) {
        Paper paper = Paper.reconstitute(
                paperId, "company-1", paperName, new BigDecimal("75"),
                new BigDecimal("70"), new BigDecimal("100"), "cm",
                coated, false, null, null, null, true,
                LocalDate.of(2026, 1, 1), LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        when(paperRepository.findById(paperId)).thenReturn(Optional.of(paper));
        when(paperPriceRepository.findByPaperId("company-1", paperId)).thenReturn(List.of(
                price("price-1", paperId, supplierId, sheetValue, preferred)
        ));
    }

    private static PaperCutLayout paperCut(String id, String paperId, String cutLayoutId) {
        return PaperCutLayout.reconstitute(
                id, "company-1", paperId, cutLayoutId,
                "vertical", new BigDecimal("2.00"), null, true,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }

    private static CutLayout cutLayout(String id, String name, int pieces) {
        return CutLayout.reconstitute(
                id, "company-1", name,
                new BigDecimal("35"), new BigDecimal("50"), "cm",
                pieces, new BigDecimal("50.00"), true, LocalDate.of(2026, 1, 1)
        );
    }

    private static PaperSupplierPrice price(
            String id,
            String paperId,
            String supplierId,
            BigDecimal sheetValue,
            boolean preferred
    ) {
        return PaperSupplierPrice.reconstitute(
                id, "company-1", paperId, supplierId,
                sheetValue, 500, BigDecimal.ZERO, null, null, null,
                LocalDate.of(2026, 1, 1), preferred, true, sheetValue,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
