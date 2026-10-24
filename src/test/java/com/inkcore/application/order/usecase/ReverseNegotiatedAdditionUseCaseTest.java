package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.model.NegotiatedAdditionMovementType;
import com.inkcore.domain.order.model.OrderCxcDocument;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.NegotiatedAdditionRepositoryPort;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReverseNegotiatedAdditionUseCaseTest {

    @Mock OrderSupport support;
    @Mock NegotiatedAdditionRepositoryPort additionRepository;
    @Mock AccountsReceivableRepositoryPort accountsReceivableRepository;
    @Mock OrderCxcDocumentService cxcDocumentService;
    @Mock Authentication authentication;

    private ReverseNegotiatedAdditionUseCase useCase;

    @BeforeEach
    void setUp() {
        CreateNegotiatedAdditionUseCase createUseCase = new CreateNegotiatedAdditionUseCase(
                support,
                additionRepository,
                accountsReceivableRepository,
                cxcDocumentService
        );
        useCase = new ReverseNegotiatedAdditionUseCase(
                support,
                additionRepository,
                createUseCase,
                cxcDocumentService
        );
    }

    @Test
    void execute_marksTheSameAdditionVoidWithoutAllocatingAnotherNumber() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 19, 12, 0);
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("op-1");
        order.setCompanyId("company-1");

        NegotiatedAddition original = new NegotiatedAddition();
        original.setOrderNegotiatedAdditionId("adn-1");
        original.setCompanyId("company-1");
        original.setAdditionNumber("ADN-3");
        original.setProductionOrderId("op-1");
        original.setMovementType(NegotiatedAdditionMovementType.REGISTRO);
        original.setAmount(new BigDecimal("180000.00"));
        original.setNotes("Troquel");

        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-1");
        when(support.now()).thenReturn(now);
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(order);
        when(additionRepository.findByIdForUpdate("company-1", "adn-1")).thenReturn(Optional.of(original));
        when(additionRepository.existsReversionFor("company-1", "adn-1")).thenReturn(false);
        when(additionRepository.update(any(NegotiatedAddition.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(additionRepository.sumNetAmount("company-1", "op-1")).thenReturn(BigDecimal.ZERO.setScale(2));
        when(accountsReceivableRepository.findByProductionOrderId("company-1", "op-1")).thenReturn(Optional.empty());
        when(support.resolveOdpNumber("company-1", "op-1")).thenReturn("ODP-4");
        when(cxcDocumentService.annulFromAddition(original, now)).thenReturn(null);

        var result = useCase.execute("op-1", "adn-1", "El cliente desistió", authentication);

        ArgumentCaptor<NegotiatedAddition> captor = ArgumentCaptor.forClass(NegotiatedAddition.class);
        verify(additionRepository).update(captor.capture());
        verify(additionRepository, never()).save(any());
        verify(support, never()).nextNegotiatedAdditionNumber(any());
        verify(cxcDocumentService).annulFromAddition(original, now);

        NegotiatedAddition saved = captor.getValue();
        assertEquals("adn-1", saved.getOrderNegotiatedAdditionId());
        assertEquals("ADN-3", saved.getAdditionNumber());
        assertEquals(NegotiatedAdditionMovementType.REGISTRO, saved.getMovementType());
        assertNull(saved.getReversedAdditionId());
        assertEquals(now, saved.getVoidedAt());
        assertEquals("user-1", saved.getVoidedBy());
        assertEquals("ADN-3", result.addition().getAdditionNumber());
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(result.negotiatedAdditionsTotal()));
    }

    @Test
    void execute_annulsLinkedCxcBeforeVoidingAddition() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 19, 12, 0);
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("op-1");
        order.setCompanyId("company-1");

        NegotiatedAddition original = new NegotiatedAddition();
        original.setOrderNegotiatedAdditionId("adn-1");
        original.setCompanyId("company-1");
        original.setAdditionNumber("ADN-3");
        original.setProductionOrderId("op-1");
        original.setMovementType(NegotiatedAdditionMovementType.REGISTRO);
        original.setAmount(new BigDecimal("180000.00"));

        OrderCxcDocument cxc = new OrderCxcDocument();
        cxc.setCxcDocumentId("cxc-1");
        cxc.setCxcNumber("CXC-9");

        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-1");
        when(support.now()).thenReturn(now);
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(order);
        when(additionRepository.findByIdForUpdate("company-1", "adn-1")).thenReturn(Optional.of(original));
        when(additionRepository.existsReversionFor("company-1", "adn-1")).thenReturn(false);
        when(additionRepository.update(any(NegotiatedAddition.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(additionRepository.sumNetAmount("company-1", "op-1")).thenReturn(BigDecimal.ZERO.setScale(2));
        when(accountsReceivableRepository.findByProductionOrderId("company-1", "op-1")).thenReturn(Optional.empty());
        when(support.resolveOdpNumber("company-1", "op-1")).thenReturn("ODP-4");
        when(cxcDocumentService.annulFromAddition(original, now)).thenReturn(cxc);

        useCase.execute("op-1", "adn-1", null, authentication);

        verify(cxcDocumentService).annulFromAddition(eq(original), eq(now));
        verify(additionRepository).update(any(NegotiatedAddition.class));
    }

    @Test
    void execute_blocksWhenLinkedCxcHasPayments() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 19, 12, 0);
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId("op-1");
        order.setCompanyId("company-1");

        NegotiatedAddition original = new NegotiatedAddition();
        original.setOrderNegotiatedAdditionId("adn-1");
        original.setCompanyId("company-1");
        original.setAdditionNumber("ADN-3");
        original.setProductionOrderId("op-1");
        original.setMovementType(NegotiatedAdditionMovementType.REGISTRO);
        original.setAmount(new BigDecimal("180000.00"));

        when(support.companyId(authentication)).thenReturn("company-1");
        when(support.userId(authentication)).thenReturn("user-1");
        when(support.now()).thenReturn(now);
        when(support.requireActiveOrder("op-1", "company-1")).thenReturn(order);
        when(additionRepository.findByIdForUpdate("company-1", "adn-1")).thenReturn(Optional.of(original));
        when(additionRepository.existsReversionFor("company-1", "adn-1")).thenReturn(false);
        doThrow(new OrderBusinessRuleException(
                "No se puede anular el adicional: la CxC CXC-9 ya tiene abonos aplicados"
        )).when(cxcDocumentService).annulFromAddition(original, now);

        assertThrows(
                OrderBusinessRuleException.class,
                () -> useCase.execute("op-1", "adn-1", null, authentication)
        );
        verify(additionRepository, never()).update(any());
    }
}
