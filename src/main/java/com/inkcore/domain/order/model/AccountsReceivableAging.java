package com.inkcore.domain.order.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Aging / alertas de cobranza derivados de {@code due_date} + saldo (no persistidos).
 */
public final class AccountsReceivableAging {

    public static final int PRE_DUE_ALERT_DAYS = 7;

    private AccountsReceivableAging() {
    }

    public static AgingSnapshot of(AccountsReceivable summary, LocalDate today) {
        if (summary == null) {
            return AgingSnapshot.empty();
        }
        BigDecimal remaining = summary.getTotalRemaining() == null ? BigDecimal.ZERO : summary.getTotalRemaining();
        boolean openBalance = remaining.compareTo(BigDecimal.ZERO) > 0;
        LocalDate dueDate = summary.getDueDate();

        if (!openBalance || dueDate == null) {
            String collectionStatus = !openBalance
                    ? (summary.getStatus() == AccountsReceivableStatus.ANULADO ? "anulada" : "al_dia")
                    : "sin_vencimiento";
            return new AgingSnapshot(
                    dueDate,
                    summary.getPaymentTermDays(),
                    summary.getOpenedAt(),
                    collectionStatus,
                    0,
                    "current",
                    false,
                    false
            );
        }

        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
        boolean overdue = daysUntilDue < 0;
        boolean dueSoon = !overdue && daysUntilDue <= PRE_DUE_ALERT_DAYS;
        int daysOverdue = overdue ? (int) Math.abs(daysUntilDue) : 0;
        String bucket = overdue ? agingBucket(daysOverdue) : "current";
        String collectionStatus = overdue ? "vencida" : (dueSoon ? "por_vencer" : "al_dia");

        return new AgingSnapshot(
                dueDate,
                summary.getPaymentTermDays(),
                summary.getOpenedAt(),
                collectionStatus,
                daysOverdue,
                bucket,
                overdue,
                dueSoon
        );
    }

    private static String agingBucket(int daysOverdue) {
        if (daysOverdue <= 30) {
            return "1-30";
        }
        if (daysOverdue <= 60) {
            return "31-60";
        }
        if (daysOverdue <= 90) {
            return "61-90";
        }
        return "90+";
    }

    public record AgingSnapshot(
            LocalDate dueDate,
            int paymentTermDays,
            LocalDateTime openedAt,
            String collectionStatus,
            int daysOverdue,
            String agingBucket,
            boolean overdue,
            boolean dueSoon
    ) {
        public static AgingSnapshot empty() {
            return new AgingSnapshot(null, 0, null, "sin_vencimiento", 0, "current", false, false);
        }
    }
}
