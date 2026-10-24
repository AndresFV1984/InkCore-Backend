package com.inkcore.infrastructure.in.rest.orders;

import com.inkcore.application.order.usecase.ListAccountsReceivableUseCase;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.PaymentMethod;
import com.inkcore.domain.order.model.PaymentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountsReceivableItemResponseTest {

    @Test
    void from_serializesAbonosNumberDistinctFromPaymentMovements() {
        ListAccountsReceivableUseCase.AccountsReceivableRow row = sampleRow("ABN-7", "LIQ-4");

        OrderResponses.AccountsReceivableItemResponse response = OrderResponses.AccountsReceivableItemResponse.from(row);

        assertEquals("CXC-7", response.cxcNumber());
        assertEquals("ABN-7", response.abonosNumber());
        assertEquals("ODP-12", response.odpNumber());
        assertEquals("LIQ-4", response.lastPaymentNumber());
        assertFalse(response.abonosNumber().equals(response.lastPaymentNumber()));
        assertTrue(response.abonosNumber().startsWith("ABN-"));
        assertTrue(response.lastPaymentNumber().startsWith("LIQ-"));
    }

    @Test
    void detailInvariant_abonosNumberNeverEqualsAnyPaymentNumber() {
        String abonosNumber = "ABN-7";
        ListAccountsReceivableUseCase.AccountsReceivableRow row = sampleRow(abonosNumber, "LIQ-4");

        List<OrderPayment> payments = List.of(
                payment("LIQ-1"),
                payment("LIQ-4"),
                payment("LIQ-9")
        );

        OrderResponses.AccountsReceivableDetailResponse detail = new OrderResponses.AccountsReceivableDetailResponse(
                OrderResponses.AccountsReceivableItemResponse.from(row),
                List.of(),
                payments.stream().map(OrderResponses.PaymentResponse::from).toList()
        );

        assertEquals(abonosNumber, detail.summary().abonosNumber());
        assertTrue(detail.payments().stream()
                .noneMatch(p -> abonosNumber.equals(p.paymentNumber())));
        assertTrue(detail.payments().stream()
                .allMatch(p -> p.paymentNumber() != null && p.paymentNumber().startsWith("LIQ-")));
    }

    @Test
    void detailInvariant_cxcNumberNeverEqualsDeliveryOrPaymentNumbers() {
        ListAccountsReceivableUseCase.AccountsReceivableRow row = sampleRow("ABN-7", "LIQ-4");
        OrderResponses.AccountsReceivableItemResponse summary =
                OrderResponses.AccountsReceivableItemResponse.from(row);

        assertEquals("CXC-7", summary.cxcNumber());
        assertEquals("ENT-3", summary.lastDeliveryNumber());
        assertFalse(summary.cxcNumber().equals(summary.lastDeliveryNumber()));
        assertFalse(summary.cxcNumber().equals(summary.lastPaymentNumber()));
        assertTrue(summary.cxcNumber().startsWith("CXC-"));
        assertTrue(summary.lastDeliveryNumber().startsWith("ENT-"));
        assertTrue(summary.lastPaymentNumber().startsWith("LIQ-"));
    }

    @Test
    void from_allowsNullLastPaymentWhenNoNetPayments() {
        ListAccountsReceivableUseCase.AccountsReceivableRow row = sampleRow("ABN-7", null);

        OrderResponses.AccountsReceivableItemResponse response = OrderResponses.AccountsReceivableItemResponse.from(row);

        assertEquals("ABN-7", response.abonosNumber());
        assertNull(response.lastPaymentNumber());
        assertNull(response.lastPaymentAt());
    }

    private static ListAccountsReceivableUseCase.AccountsReceivableRow sampleRow(
            String abonosNumber,
            String lastPaymentNumber
    ) {
        return new ListAccountsReceivableUseCase.AccountsReceivableRow(
                "ar-1",
                "CXC-7",
                abonosNumber,
                "op-1",
                "OP-142",
                "ODP-12",
                "client-1",
                "ACME",
                1000,
                500,
                500,
                new BigDecimal("1500000.00"),
                lastPaymentNumber == null ? BigDecimal.ZERO : new BigDecimal("300000.00"),
                lastPaymentNumber == null ? new BigDecimal("1500000.00") : new BigDecimal("1200000.00"),
                lastPaymentNumber == null ? BigDecimal.ZERO : new BigDecimal("250000.00"),
                lastPaymentNumber == null ? BigDecimal.ZERO : new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                lastPaymentNumber == null ? "pendiente" : "parcial",
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 5, 14, 30),
                LocalDate.of(2026, 10, 5),
                30,
                "al_dia",
                0,
                "current",
                false,
                false,
                "ENT-3",
                LocalDateTime.of(2026, 9, 12, 11, 0),
                lastPaymentNumber,
                lastPaymentNumber == null ? null : LocalDateTime.of(2026, 9, 15, 16, 0)
        );
    }

    private static OrderPayment payment(String paymentNumber) {
        OrderPayment payment = new OrderPayment();
        payment.setOrderPaymentId("pay-" + paymentNumber);
        payment.setPaymentNumber(paymentNumber);
        payment.setProductionOrderId("op-1");
        payment.setPaymentType(PaymentType.ABONO);
        payment.setPaymentMethod(PaymentMethod.TRANSFERENCIA);
        payment.setAmount(new BigDecimal("100000.00"));
        return payment;
    }
}
