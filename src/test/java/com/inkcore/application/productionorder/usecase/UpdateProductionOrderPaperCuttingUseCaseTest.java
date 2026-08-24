package com.inkcore.application.productionorder.usecase;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.model.PaperTypeCutAssignment;
import com.inkcore.domain.papertype.ports.out.PaperTypeRepositoryPort;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductionOrderPaperCuttingUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-22T12:00:00Z");

    @Mock ProductionOrderRepositoryPort repository;
    @Mock AuthenticatedCompanyResolver companyResolver;
    @Mock PaperTypeRepositoryPort paperTypeRepository;
    @Mock CutLayoutRepositoryPort cutLayoutRepository;

    private UpdateProductionOrderPaperCuttingUseCase useCase;

    @BeforeEach
    void setUp() {
        ProductionOrderSupport support = new ProductionOrderSupport(
                repository,
                companyResolver,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
        useCase = new UpdateProductionOrderPaperCuttingUseCase(support, paperTypeRepository, cutLayoutRepository);
    }

    @Test
    void execute_persistsCatalogSnapshotsEvenWhenClientSuppliesCutPaper() {
        var auth = new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-1");
        when(companyResolver.resolveUserId(auth)).thenReturn("user-1");

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

        when(repository.findById("order-1")).thenReturn(Optional.of(order));
        when(repository.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PaperType paper = PaperType.reconstitute(
                "papel-1", "company-1", "Bond",
                new BigDecimal("70"), new BigDecimal("100"), "cm",
                new BigDecimal("120"), 500, false, true, LocalDate.of(2026, 1, 1),
                List.of(PaperTypeCutAssignment.of("corte-1", new BigDecimal("50")))
        );
        when(paperTypeRepository.findById("papel-1")).thenReturn(Optional.of(paper));

        CutLayout cut = CutLayout.reconstitute(
                "corte-1", "company-1", "2x2",
                new BigDecimal("35"), new BigDecimal("50"), "cm",
                4, true, LocalDate.of(2026, 1, 1)
        );
        when(cutLayoutRepository.findById("corte-1")).thenReturn(Optional.of(cut));

        useCase.execute(
                "order-1",
                new UpdateProductionOrderPaperCuttingCommand(
                        2L,
                        true,
                        2,
                        true,
                        null,
                        "%",
                        BigDecimal.ZERO,
                        List.of(new UpdateProductionOrderPaperCuttingCommand.PaperRowInput(
                                null,
                                "plate-srv-1",
                                null,
                                "plate-srv-1",
                                false,
                                null,
                                true,
                                "papel-1",
                                "corte-1",
                                true,
                                100,
                                null,
                                null
                        ))
                ),
                auth
        );

        ArgumentCaptor<ProductionOrder> captor = ArgumentCaptor.forClass(ProductionOrder.class);
        verify(repository).save(captor.capture());
        List<PaperRow> rows = captor.getValue().getPaperRows();
        assertEquals(1, rows.size());
        PaperRow saved = rows.get(0);
        assertEquals("plate-srv-1", saved.getPlateId());
        assertEquals("papel-1", saved.getPaperTypeId());
        assertEquals("Bond", saved.getPaperName());
        assertEquals("corte-1", saved.getCutLayoutId());
        assertEquals("2x2", saved.getCutLayoutName());
        assertEquals(4, saved.getPiecesPerSheet());
        assertEquals(0, new BigDecimal("50").compareTo(saved.getCutValue()));
    }
}
