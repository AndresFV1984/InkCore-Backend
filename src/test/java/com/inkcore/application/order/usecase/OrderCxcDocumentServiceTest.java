package com.inkcore.application.order.usecase;

import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.model.CxcDocumentConcept;
import com.inkcore.domain.order.model.CxcDocumentStatus;
import com.inkcore.domain.order.model.OrderCxcDocument;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.OrderPaymentApplication;
import com.inkcore.domain.order.ports.out.OrderCxcDocumentRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderPaymentApplicationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCxcDocumentServiceTest {

    @Mock OrderCxcDocumentRepositoryPort cxcDocumentRepository;
    @Mock OrderPaymentApplicationRepositoryPort applicationRepository;

    private OrderCxcDocumentService service;

    @BeforeEach
    void setUp() {
        service = new OrderCxcDocumentService(cxcDocumentRepository, applicationRepository);
    }

    @Test
    void applyPayment_requiresCxcWhenOpenDocumentsExist() {
        OrderPayment payment = payment(new BigDecimal("100000.00"));
        when(cxcDocumentRepository.findOpenByProductionOrderAndConcept(
                "company-1", "op-1", CxcDocumentConcept.ENTREGA))
                .thenReturn(List.of(openCxc("cxc-1", new BigDecimal("500000.00"))));

        assertThrows(
                OrderBusinessRuleException.class,
                () -> service.applyPayment(payment, null, null, LocalDateTime.now())
        );
        verify(applicationRepository, never()).saveAll(anyList());
    }

    @Test
    void applyPayment_splitsToCxcAndFavor() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 10, 0);
        OrderPayment payment = payment(new BigDecimal("300000.00"));
        OrderCxcDocument cxc = openCxc("cxc-1", new BigDecimal("200000.00"));

        when(cxcDocumentRepository.findOpenByProductionOrderAndConcept(
                "company-1", "op-1", CxcDocumentConcept.ENTREGA))
                .thenReturn(List.of(cxc));
        when(cxcDocumentRepository.findByIdForUpdate("company-1", "cxc-1"))
                .thenReturn(Optional.of(cxc));
        when(cxcDocumentRepository.sumApplied("company-1", "cxc-1")).thenReturn(BigDecimal.ZERO);
        when(applicationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<OrderPaymentApplication> apps = service.applyPayment(payment, "cxc-1", null, now);

        assertEquals(2, apps.size());
        assertEquals("cxc-1", apps.get(0).getCxcDocumentId());
        assertEquals(0, new BigDecimal("200000.00").compareTo(apps.get(0).getAmount()));
        assertNull(apps.get(1).getCxcDocumentId());
        assertEquals(0, new BigDecimal("100000.00").compareTo(apps.get(1).getAmount()));
        verify(cxcDocumentRepository).update(any(OrderCxcDocument.class));
    }

    @Test
    void applyPayment_allToFavorWhenNoOpenCxc() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 10, 0);
        OrderPayment payment = payment(new BigDecimal("50000.00"));
        when(cxcDocumentRepository.findOpenByProductionOrderAndConcept(
                "company-1", "op-1", CxcDocumentConcept.ENTREGA))
                .thenReturn(List.of());
        when(applicationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<OrderPaymentApplication> apps = service.applyPayment(payment, null, null, now);

        assertEquals(1, apps.size());
        assertNull(apps.get(0).getCxcDocumentId());
        assertEquals(0, new BigDecimal("50000.00").compareTo(apps.get(0).getAmount()));
        verify(cxcDocumentRepository, never()).update(any());
    }

    @Test
    void applyFavorToCxc_consumesFavorFifo() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 8, 10, 0);
        OrderCxcDocument cxc = openCxc("cxc-1", new BigDecimal("100000.00"));
        OrderPaymentApplication favor = new OrderPaymentApplication();
        favor.setApplicationId("fav-1");
        favor.setPaymentId("pay-1");
        favor.setAmount(new BigDecimal("80000.00"));

        when(applicationRepository.sumActiveFavor("company-1", "op-1"))
                .thenReturn(new BigDecimal("80000.00"));
        when(cxcDocumentRepository.findByIdForUpdate("company-1", "cxc-1"))
                .thenReturn(Optional.of(cxc));
        when(cxcDocumentRepository.sumApplied("company-1", "cxc-1")).thenReturn(BigDecimal.ZERO);
        when(applicationRepository.findActiveFavorByProductionOrderId("company-1", "op-1"))
                .thenReturn(List.of(favor));
        when(applicationRepository.findByPaymentId("company-1", "pay-1")).thenReturn(List.of(favor));
        when(applicationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<OrderPaymentApplication> apps = service.applyFavorToCxc(
                "company-1", "op-1", "cxc-1", new BigDecimal("50000.00"), null, now);

        assertEquals(1, apps.size());
        assertEquals("cxc-1", apps.get(0).getCxcDocumentId());
        assertEquals("fav-1", apps.get(0).getOriginApplicationId());
        assertEquals("pay-1", apps.get(0).getPaymentId());
        ArgumentCaptor<OrderCxcDocument> captor = ArgumentCaptor.forClass(OrderCxcDocument.class);
        verify(cxcDocumentRepository).update(captor.capture());
        assertEquals(CxcDocumentStatus.PARCIAL, captor.getValue().getStatus());
    }

    private static OrderPayment payment(BigDecimal amount) {
        OrderPayment payment = new OrderPayment();
        payment.setOrderPaymentId("pay-1");
        payment.setCompanyId("company-1");
        payment.setProductionOrderId("op-1");
        payment.setPaymentConcept(CxcDocumentConcept.ENTREGA);
        payment.setAmount(amount);
        payment.setPaidAt(LocalDateTime.of(2026, 9, 8, 10, 0));
        return payment;
    }

    private static OrderCxcDocument openCxc(String id, BigDecimal total) {
        OrderCxcDocument doc = new OrderCxcDocument();
        doc.setCxcDocumentId(id);
        doc.setCompanyId("company-1");
        doc.setProductionOrderId("op-1");
        doc.setConcept(CxcDocumentConcept.ENTREGA);
        doc.setTotal(total);
        doc.setStatus(CxcDocumentStatus.PENDIENTE);
        doc.setCxcNumber("CXC-1");
        return doc;
    }
}
