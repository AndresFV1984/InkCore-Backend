package com.inkcore.application.order;

import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.AccountsReceivableStatus;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.service.ProductionOrderCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Totales del módulo Abonos: base = {@code totalToCharge} de la OP (panel Cobro),
 * no el valor acumulado de entregas (CxC).
 * <p>
 * {@code totalRemaining} puede ser negativo (saldo a favor).
 */
public final class AbonosBalance {

    private AbonosBalance() {
    }

    public record Snapshot(BigDecimal totalOwed, BigDecimal totalPaid, BigDecimal totalRemaining) {
    }

    /**
     * @param order     OP con agregado de costos (para {@link ProductionOrderCalculator#calculateTotalToCharge})
     * @param totalPaid suma de abonos/anticipos/retenciones vigentes; null → 0
     */
    public static Snapshot of(ProductionOrder order, BigDecimal totalPaid) {
        BigDecimal owed = ProductionOrderCalculator.calculateTotalToCharge(order);
        if (owed == null) {
            owed = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            owed = owed.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal paid = totalPaid == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalPaid.setScale(2, RoundingMode.HALF_UP);
        return new Snapshot(owed, paid, owed.subtract(paid));
    }

    /** Sobrescribe totalOwed/totalRemaining (y status Abonos) sobre el agregado persistido. */
    public static void applyTo(AccountsReceivable summary, ProductionOrder order) {
        if (summary == null) {
            return;
        }
        Snapshot snapshot = of(order, summary.getTotalPaid());
        summary.setTotalOwed(snapshot.totalOwed());
        summary.setTotalRemaining(snapshot.totalRemaining());
        summary.setStatus(resolveStatus(summary, snapshot));
    }

    public static AccountsReceivableStatus resolveStatus(AccountsReceivable summary, Snapshot snapshot) {
        if (summary != null && summary.getStatus() == AccountsReceivableStatus.ANULADO) {
            return AccountsReceivableStatus.ANULADO;
        }
        BigDecimal owed = snapshot.totalOwed();
        BigDecimal paid = snapshot.totalPaid();
        int delivered = summary == null ? 0 : summary.getDeliveredUnits();
        if (delivered == 0 && owed.compareTo(BigDecimal.ZERO) == 0 && paid.compareTo(BigDecimal.ZERO) == 0) {
            return AccountsReceivableStatus.PENDIENTE;
        }
        if (owed.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(owed) >= 0) {
            return AccountsReceivableStatus.PAGADO;
        }
        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            return AccountsReceivableStatus.PARCIAL;
        }
        return AccountsReceivableStatus.PENDIENTE;
    }
}
