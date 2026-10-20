package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.model.NegotiatedAdditionMovementType;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.NegotiatedAdditionRepositoryPort;
import com.inkcore.domain.productionorder.model.PrepressDetails;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateNegotiatedAdditionUseCaseTest {

    @Mock OrderSupport support;
    @Mock NegotiatedAdditionRepositoryPort additionRepository;
    @Mock AccountsReceivableRepositoryPort accountsReceivableRepository;
    @Mock Authentication authentication;

    private CreateNegotiatedAdditionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateNegotiatedAdditionUseCase(support, additionRepository, accountsReceivableRepository);
    }

    @Test
    void execute_registersApprovedScopeChangeAndRaisesAmountToCollect() {
        ProductionOrder order = orderWithCharge(new BigDecimal("1500000.00"));
        LocalDateTime now = LocalDateTime.of(2026, 10, 19, 11, 30);
        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-1");
        when(support.now()).thenReturn(now);
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(order);
        when(support.nextNegotiatedAdditionNumber("company-1")).thenReturn("ADN-1");
        when(additionRepository.save(any(NegotiatedAddition.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(additionRepository.sumNetAmount("company-1", "op-1")).thenReturn(new BigDecimal("180000.00"));
        when(accountsReceivableRepository.findByProductionOrderId("company-1", "op-1")).thenReturn(Optional.empty());
        when(support.resolveOdpNumber("company-1", "op-1")).thenReturn("ODP-4");

        var result = useCase.execute(
                new CreateNegotiatedAdditionUseCase.CreateNegotiatedAdditionCommand(
                        "op-1",
                        "  Troquel adicional  ",
                        "solicitud_cliente",
                        new BigDecimal("180000"),
                        2,
                        " María Gómez ",
                        "correo",
                        " Correo 19/10 ",
                        null,
                        "  "
                ),
                authentication
        );

        ArgumentCaptor<NegotiatedAddition> captor = ArgumentCaptor.forClass(NegotiatedAddition.class);
        verify(additionRepository).save(captor.capture());
        NegotiatedAddition saved = captor.getValue();
        assertEquals("ADN-1", saved.getAdditionNumber());
        assertEquals(NegotiatedAdditionMovementType.REGISTRO, saved.getMovementType());
        assertEquals("Troquel adicional", saved.getConcept());
        assertEquals("María Gómez", saved.getApprovedByClientName());
        assertEquals(com.inkcore.domain.order.model.NegotiatedAdditionReason.SOLICITUD_CLIENTE, saved.getChangeReason());
        assertEquals(com.inkcore.domain.order.model.NegotiatedAdditionApprovalChannel.CORREO, saved.getApprovalChannel());
        assertEquals("Correo 19/10", saved.getApprovalReference());
        assertEquals(2, saved.getScheduleImpactDays());
        assertEquals("user-1", saved.getRegisteredBy());
        assertEquals(now, saved.getApprovedAt());
        assertEquals(0, new BigDecimal("180000.00").compareTo(saved.getAmount()));
        assertEquals(0, new BigDecimal("1500000.00").compareTo(result.productionTotal()));
        assertEquals(0, new BigDecimal("1680000.00").compareTo(result.amountToCollect()));
        assertEquals(0, new BigDecimal("1680000.00").compareTo(result.accountsReceivable().getTotalOwed()));
        assertEquals("ODP-4", result.odpNumber());
    }

    @Test
    void execute_rejectsBlankConcept() {
        assertThrows(OrderBusinessRuleException.class, () -> useCase.execute(
                new CreateNegotiatedAdditionUseCase.CreateNegotiatedAdditionCommand(
                        "op-1", " ", "solicitud_cliente", new BigDecimal("10"), 0, "Cliente", "correo", null, null, null
                ),
                authentication
        ));
    }

    private static ProductionOrder orderWithCharge(BigDecimal amount) {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("op-1");
        order.setCompanyId("company-1");
        order.setClientId("client-1");
        PrepressDetails prepress = new PrepressDetails();
        prepress.setTotalPlatesValue(amount);
        order.setPrepress(prepress);
        return order;
    }
}
