package com.inkcore.application.order;

import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.AccountsReceivableStatus;
import com.inkcore.domain.productionorder.model.PrepressDetails;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbonosBalanceTest {

    @Test
    void of_usesTotalToChargeMinusPaid() {
        ProductionOrder order = orderWithTotalToCharge(new BigDecimal("1500000.00"));

        AbonosBalance.Snapshot snapshot = AbonosBalance.of(order, new BigDecimal("300000.00"));

        assertEquals(0, new BigDecimal("1500000.00").compareTo(snapshot.totalOwed()));
        assertEquals(0, new BigDecimal("300000.00").compareTo(snapshot.totalPaid()));
        assertEquals(0, new BigDecimal("1200000.00").compareTo(snapshot.totalRemaining()));
    }

    @Test
    void of_allowsNegativeRemainingWhenPaidExceedsOpTotal() {
        ProductionOrder order = orderWithTotalToCharge(new BigDecimal("1000000.00"));

        AbonosBalance.Snapshot snapshot = AbonosBalance.of(order, new BigDecimal("1200000.00"));

        assertEquals(0, new BigDecimal("-200000.00").compareTo(snapshot.totalRemaining()));
    }

    @Test
    void of_withoutCostData_treatsOwedAsZeroForAdvance() {
        ProductionOrder order = ProductionOrder.reconstitute();

        AbonosBalance.Snapshot snapshot = AbonosBalance.of(order, new BigDecimal("250000.00"));

        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(snapshot.totalOwed()));
        assertEquals(0, new BigDecimal("-250000.00").compareTo(snapshot.totalRemaining()));
    }

    @Test
    void applyTo_overridesPersistedDeliveryBasedTotals() {
        ProductionOrder order = orderWithTotalToCharge(new BigDecimal("1500000.00"));
        AccountsReceivable summary = new AccountsReceivable();
        summary.setDeliveredUnits(0);
        summary.setTotalOwed(BigDecimal.ZERO);
        summary.setTotalPaid(new BigDecimal("300000.00"));
        summary.setTotalRemaining(new BigDecimal("-300000.00"));
        summary.setStatus(AccountsReceivableStatus.PARCIAL);

        AbonosBalance.applyTo(summary, order);

        assertEquals(0, new BigDecimal("1500000.00").compareTo(summary.getTotalOwed()));
        assertEquals(0, new BigDecimal("1200000.00").compareTo(summary.getTotalRemaining()));
        assertEquals(AccountsReceivableStatus.PARCIAL, summary.getStatus());
    }

    private static ProductionOrder orderWithTotalToCharge(BigDecimal amount) {
        ProductionOrder order = ProductionOrder.reconstitute();
        PrepressDetails prepress = new PrepressDetails();
        prepress.setTotalPlatesValue(amount);
        order.setPrepress(prepress);
        return order;
    }
}
