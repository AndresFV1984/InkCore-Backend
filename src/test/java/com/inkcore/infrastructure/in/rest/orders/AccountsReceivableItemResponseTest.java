package com.inkcore.infrastructure.in.rest.orders;

import com.inkcore.application.order.usecase.ListAccountsReceivableUseCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AccountsReceivableItemResponseTest {

    @Test
    void from_serializesLastPaymentNumberAndTotals() {
        ListAccountsReceivableUseCase.AccountsReceivableRow row = new ListAccountsReceivableUseCase.AccountsReceivableRow(
                "ar-1",
                "CXC-7",
                "op-1",
                "OP-142",
                "client-1",
                "ACME",
                1000,
                500,
                500,
                new BigDecimal("600000.00"),
                new BigDecimal("200000.00"),
                new BigDecimal("400000.00"),
                new BigDecimal("150000.00"),
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                "parcial",
                LocalDateTime.of(2026, 9, 5, 14, 30),
                LocalDate.of(2026, 10, 5),
                30,
                "al_dia",
                0,
                "current",
                false,
                false,
                "ODP-3",
                LocalDateTime.of(2026, 9, 12, 11, 0),
                "ABN-4",
                LocalDateTime.of(2026, 9, 15, 16, 0)
        );

        OrderResponses.AccountsReceivableItemResponse response = OrderResponses.AccountsReceivableItemResponse.from(row);

        assertEquals("CXC-7", response.cxcNumber());
        assertEquals("ABN-4", response.lastPaymentNumber());
        assertEquals("2026-09-15T16:00", response.lastPaymentAt());
        assertEquals(0, new BigDecimal("200000.00").compareTo(response.totalPaid()));
        assertEquals(0, new BigDecimal("400000.00").compareTo(response.totalRemaining()));
    }

    @Test
    void from_allowsNullLastPaymentWhenNoNetPayments() {
        ListAccountsReceivableUseCase.AccountsReceivableRow row = new ListAccountsReceivableUseCase.AccountsReceivableRow(
                "ar-1",
                "CXC-7",
                "op-1",
                "OP-142",
                "client-1",
                "ACME",
                1000,
                500,
                500,
                new BigDecimal("600000.00"),
                BigDecimal.ZERO,
                new BigDecimal("600000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "pendiente",
                LocalDateTime.of(2026, 9, 5, 14, 30),
                LocalDate.of(2026, 10, 5),
                30,
                "al_dia",
                0,
                "current",
                false,
                false,
                "ODP-3",
                LocalDateTime.of(2026, 9, 12, 11, 0),
                null,
                null
        );

        OrderResponses.AccountsReceivableItemResponse response = OrderResponses.AccountsReceivableItemResponse.from(row);

        assertNull(response.lastPaymentNumber());
        assertNull(response.lastPaymentAt());
    }
}
