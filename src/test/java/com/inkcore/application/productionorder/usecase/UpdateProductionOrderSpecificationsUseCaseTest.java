package com.inkcore.application.productionorder.usecase;

import com.inkcore.application.shared.AuthenticatedCompanyResolver;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.productionorder.model.OperatorAssignment;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort;
import com.inkcore.domain.seller.ports.out.SellerRepositoryPort;
import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductionOrderSpecificationsUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-22T12:00:00Z");

    @Mock ProductionOrderRepositoryPort repository;
    @Mock AuthenticatedCompanyResolver companyResolver;
    @Mock ClientRepositoryPort clientRepository;
    @Mock SellerRepositoryPort sellerRepository;
    @Mock UserRepositoryPort userRepository;

    private UpdateProductionOrderSpecificationsUseCase useCase;

    @BeforeEach
    void setUp() {
        ProductionOrderSupport support = new ProductionOrderSupport(
                repository,
                companyResolver,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
        useCase = new UpdateProductionOrderSpecificationsUseCase(
                support,
                new ProductionOrderOperatorsApplier(userRepository),
                clientRepository,
                sellerRepository
        );
    }

    @Test
    void execute_persistsOperatorsReplaceAll() {
        var auth = new UsernamePasswordAuthenticationToken("actor", "n/a", List.of());
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-1");
        when(companyResolver.resolveUserId(auth)).thenReturn("actor");

        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("order-1");
        order.setCompanyId("company-1");
        order.setVersion(3L);
        order.setClientId("client-1");
        order.setWorkName("Trabajo");
        order.setOrderDate(LocalDate.of(2026, 8, 15));
        order.setRequestedQuantity(1000);
        order.replaceOperators(List.of(
                OperatorAssignment.of("company-1", "order-1", ProductionOrderStage.BILLING, "old-user")
        ));

        when(repository.findById("order-1")).thenReturn(Optional.of(order));
        when(repository.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(sampleClient()));
        when(userRepository.findById("user-pre")).thenReturn(Optional.of(sampleUser("user-pre")));
        when(userRepository.findById("user-print")).thenReturn(Optional.of(sampleUser("user-print")));

        useCase.execute(
                "order-1",
                new UpdateProductionOrderSpecificationsCommand(
                        3L,
                        "client-1",
                        "Trabajo",
                        null,
                        LocalDate.of(2026, 8, 15),
                        1000,
                        null,
                        null,
                        List.of(
                                new OperatorAssignmentCommand("PREPRESS", "user-pre", null),
                                new OperatorAssignmentCommand("PRINTING", "user-print", "OPERARIO")
                        ),
                        null
                ),
                auth
        );

        ArgumentCaptor<ProductionOrder> captor = ArgumentCaptor.forClass(ProductionOrder.class);
        verify(repository).save(captor.capture());
        List<OperatorAssignment> operators = captor.getValue().getOperators();
        assertEquals(2, operators.size());
        assertTrue(operators.stream().noneMatch(o -> o.getStage() == ProductionOrderStage.BILLING));
        assertEquals("user-pre", operators.stream()
                .filter(o -> o.getStage() == ProductionOrderStage.PREPRESS)
                .findFirst().orElseThrow().getUserId());
    }

    @Test
    void execute_withoutOperatorsKey_preservesExisting() {
        var auth = new UsernamePasswordAuthenticationToken("actor", "n/a", List.of());
        when(companyResolver.resolveCompanyId(auth)).thenReturn("company-1");
        when(companyResolver.resolveUserId(auth)).thenReturn("actor");

        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("order-1");
        order.setCompanyId("company-1");
        order.setVersion(3L);
        order.setClientId("client-1");
        order.setWorkName("Trabajo");
        order.setOrderDate(LocalDate.of(2026, 8, 15));
        order.setRequestedQuantity(1000);
        order.replaceOperators(List.of(
                OperatorAssignment.of("company-1", "order-1", ProductionOrderStage.PRINTING, "kept-user")
        ));

        when(repository.findById("order-1")).thenReturn(Optional.of(order));
        when(repository.save(any(ProductionOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.findById("client-1")).thenReturn(Optional.of(sampleClient()));

        useCase.execute(
                "order-1",
                new UpdateProductionOrderSpecificationsCommand(
                        3L,
                        "client-1",
                        "Trabajo actualizado",
                        null,
                        LocalDate.of(2026, 8, 15),
                        1200,
                        null,
                        null,
                        null,
                        null
                ),
                auth
        );

        ArgumentCaptor<ProductionOrder> captor = ArgumentCaptor.forClass(ProductionOrder.class);
        verify(repository).save(captor.capture());
        assertEquals(1, captor.getValue().getOperators().size());
        assertEquals("kept-user", captor.getValue().getOperators().get(0).getUserId());
        assertEquals(ProductionOrderStage.PRINTING, captor.getValue().getOperators().get(0).getStage());
    }

    private static Client sampleClient() {
        return Client.reconstitute(
                "client-1", "company-1", "Cliente", "NIT", "900",
                null, null, null, null, null, null, true, LocalDate.of(2026, 1, 1)
        );
    }

    private static User sampleUser(String userId) {
        UUID roleId = UUID.fromString("b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22");
        return User.reconstitute(
                userId, "company-1", "1", "CC", "Op", userId + "@test.com", "",
                "Antioquia", "Medellin", "", "hash", LocalDate.of(2026, 1, 1),
                true, 1L, List.of(roleId), List.of("Operario"), List.of("OPERARIO"),
                List.of(), false, null, null, 0, null, null
        );
    }
}
