package com.inkcore.infrastructure.in.rest.orders;

import com.inkcore.application.order.usecase.CreateOrderDeliveryUseCase;
import com.inkcore.application.order.usecase.CreateOrderPaymentUseCase;
import com.inkcore.application.order.usecase.GetClientAccountsReceivableUseCase;
import com.inkcore.application.order.usecase.GetOrderAccountsReceivableUseCase;
import com.inkcore.application.order.usecase.GetOrderAvailabilityUseCase;
import com.inkcore.application.order.usecase.ListAccountsReceivableUseCase;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.AccountsReceivableAging;
import com.inkcore.domain.order.model.DeliveryMovementType;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.model.OrderPayment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class OrderResponses {

    private OrderResponses() {
    }

    @Schema(name = "OrderAvailabilityResponse", description = "Disponibilidad comercial = procesado (estación) − entregado")
    public record AvailabilityResponse(
            @Schema(description = "Unidades procesadas (station_order_progress.cantidad_disponible)", example = "800")
            int processed,
            @Schema(description = "Unidades ya entregadas (accounts_receivable.delivered_units)", example = "500")
            int delivered,
            @Schema(description = "Unidades disponibles para nueva entrega", example = "300")
            int available
    ) {
        public static AvailabilityResponse from(GetOrderAvailabilityUseCase.Availability availability) {
            return new AvailabilityResponse(
                    availability.processed(), availability.delivered(), availability.available());
        }
    }

    @Schema(name = "OrderAccountsReceivableResponse", description = "Snapshot CxC/Abonos de una OP. "
            + "cxcNumber = id CxC (CXC-n). abonosNumber = id del agregado de Abonos (ABN-n), distinto de paymentNumber "
            + "(misma familia ABN-, secuencia compartida). "
            + "Abonos: totalOwed = totalToCharge de la OP; totalRemaining = totalOwed − totalPaid (puede ser negativo). "
            + "Cartera CxC por entregas: deliveredUnits/pendingUnits + ledger de entregas. "
            + "odpNumber = pedido comercial. lastPaymentNumber = último movimiento ABN (no id de cuenta).")
    public record AccountsReceivableResponse(
            @Schema(description = "UUID de la CxC; null si status=sin_movimientos", example = "ar-uuid-001") String accountsReceivableId,
            @Schema(description = "Número visible CXC-{n}; null si aún no hay movimientos", example = "CXC-7") String cxcNumber,
            @Schema(
                    description = "Id de negocio del agregado de Abonos (ABN-{n}). Inmutable tras el primer INSERT del trigger. "
                            + "Misma familia ABN- que paymentNumber; valor distinto (secuencia compartida). ≠ lastPaymentNumber.",
                    example = "ABN-7"
            )
            String abonosNumber,
            @Schema(description = "Orden de producción consultada", example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd")
            String productionOrderId,
            @Schema(
                    description = "Nº pedido comercial (customer_orders.odp_number = ODP-{n}). Asociado 1:1; no es el id del agregado de Abonos.",
                    example = "ODP-12"
            )
            String odpNumber,
            @Schema(description = "Cliente de la orden", example = "client-seed-001")
            String clientId,
            @Schema(example = "1000") int totalUnits,
            @Schema(example = "500") int deliveredUnits,
            @Schema(example = "500") int pendingUnits,
            @Schema(
                    description = "Total a cobrar de la OP (totalToCharge / panel Cobro). Base del módulo Abonos; "
                            + "no es el valor acumulado de entregas.",
                    example = "1500000.00"
            ) BigDecimal totalOwed,
            @Schema(description = "Suma de abonos/anticipos/retenciones vigentes", example = "300000.00") BigDecimal totalPaid,
            @Schema(
                    description = "totalOwed − totalPaid. Puede ser negativo (saldo a favor).",
                    example = "1200000.00"
            ) BigDecimal totalRemaining,
            @Schema(example = "150000.00") BigDecimal totalCashPaid,
            @Schema(example = "50000.00") BigDecimal totalWithheld,
            @Schema(example = "0.00") BigDecimal totalAdvancePaid,
            @Schema(allowableValues = {"sin_movimientos", "pendiente", "parcial", "pagado", "anulado"}, example = "parcial")
            String status,
            @Schema(description = "False cuando accounts_receivable aún no tiene fila y la respuesta es transitoria", example = "true")
            boolean hasMovements,
            @Schema(description = "Apertura de deuda: deliveredAt de la 1ª entrega (no se actualiza en entregas posteriores)", example = "2026-09-05T14:30:00")
            String openedAt,
            @Schema(description = "Fecha límite de pago", example = "2026-10-05")
            String dueDate,
            @Schema(description = "Días de crédito snapshot", example = "30")
            int paymentTermDays,
            @Schema(allowableValues = {"al_dia", "por_vencer", "vencida", "sin_vencimiento", "anulada"}, example = "por_vencer")
            String collectionStatus,
            @Schema(example = "0") int daysOverdue,
            @Schema(allowableValues = {"current", "1-30", "31-60", "61-90", "90+"}, example = "current")
            String agingBucket,
            @Schema(example = "true") boolean dueSoon,
            @Schema(example = "false") boolean overdue,
            @Schema(description = "Última entrega registrada; null si no existe. Distinto de openedAt (1ª entrega).", example = "2026-09-05T14:30:00")
            String lastDeliveryAt,
            @Schema(
                    description = "Último paymentNumber (ABN-{n}) vigente de la OP. Null si no hay liquidaciones netas. "
                            + "Análogo a lastDeliveryNumber; no es abonosNumber (ABN-{n}) ni cxcNumber.",
                    example = "ABN-4"
            )
            String lastPaymentNumber,
            @Schema(description = "paidAt del último abono vigente; null si no hay liquidaciones netas", example = "2026-09-05T16:00:00")
            String lastPaymentAt
    ) {
        public static AccountsReceivableResponse from(AccountsReceivable summary) {
            return from(summary, null);
        }

        public static AccountsReceivableResponse from(AccountsReceivable summary, String odpNumber) {
            AccountsReceivableAging.AgingSnapshot aging = AccountsReceivableAging.of(summary, LocalDate.now());
            return new AccountsReceivableResponse(
                    summary.getAccountsReceivableId(),
                    summary.getCxcNumber(),
                    summary.getAbonosNumber(),
                    summary.getProductionOrderId(),
                    odpNumber,
                    summary.getClientId(),
                    summary.getTotalUnits(),
                    summary.getDeliveredUnits(),
                    summary.getPendingUnits(),
                    summary.getTotalOwed(),
                    summary.getTotalPaid(),
                    summary.getTotalRemaining(),
                    nullToZero(summary.getTotalCashPaid()),
                    nullToZero(summary.getTotalWithheld()),
                    nullToZero(summary.getTotalAdvancePaid()),
                    summary.getStatus() == null ? null : summary.getStatus().getDbValue(),
                    true,
                    summary.getOpenedAt() == null ? null : summary.getOpenedAt().toString(),
                    summary.getDueDate() == null ? null : summary.getDueDate().toString(),
                    summary.getPaymentTermDays(),
                    aging.collectionStatus(),
                    aging.daysOverdue(),
                    aging.agingBucket(),
                    aging.dueSoon(),
                    aging.overdue(),
                    summary.getLastDeliveryAt() == null ? null : summary.getLastDeliveryAt().toString(),
                    summary.getLastPaymentNumber(),
                    summary.getLastPaymentAt() == null ? null : summary.getLastPaymentAt().toString()
            );
        }

        public static AccountsReceivableResponse from(GetOrderAccountsReceivableUseCase.AccountsReceivableView summary) {
            return new AccountsReceivableResponse(
                    summary.accountsReceivableId(),
                    summary.cxcNumber(),
                    summary.abonosNumber(),
                    summary.productionOrderId(),
                    summary.odpNumber(),
                    summary.clientId(),
                    summary.totalUnits(),
                    summary.deliveredUnits(),
                    summary.pendingUnits(),
                    summary.totalOwed(),
                    summary.totalPaid(),
                    summary.totalRemaining(),
                    nullToZero(summary.totalCashPaid()),
                    nullToZero(summary.totalWithheld()),
                    nullToZero(summary.totalAdvancePaid()),
                    summary.status(),
                    summary.hasMovements(),
                    summary.openedAt() == null ? null : summary.openedAt().toString(),
                    summary.dueDate() == null ? null : summary.dueDate().toString(),
                    summary.paymentTermDays(),
                    summary.collectionStatus(),
                    summary.daysOverdue(),
                    summary.agingBucket(),
                    summary.dueSoon(),
                    summary.overdue(),
                    summary.lastDeliveryAt() == null ? null : summary.lastDeliveryAt().toString(),
                    summary.lastPaymentNumber(),
                    summary.lastPaymentAt() == null ? null : summary.lastPaymentAt().toString()
            );
        }

        private static BigDecimal nullToZero(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    @Schema(name = "OrderCreateDeliveryResponse", description = "Resultado de registrar una entrega o su reversión. "
            + "accountsReceivable incluye cxcNumber, abonosNumber (ABN-n) y odpNumber cuando aplica.")
    public record CreateDeliveryResponse(
            DeliveryResponse delivery,
            AccountsReceivableResponse accountsReceivable
    ) {
        public static CreateDeliveryResponse from(CreateOrderDeliveryUseCase.CreateDeliveryResult result) {
            return new CreateDeliveryResponse(
                    DeliveryResponse.from(result.delivery()),
                    AccountsReceivableResponse.from(result.accountsReceivable(), result.odpNumber())
            );
        }
    }

    @Schema(name = "OrderDeliveryResponse", description = "Entrega o reversión comercial (ledger append-only). "
            + "deliveryNumber=ODP-{n} es el consecutivo de la entrega (order_deliveries), "
            + "no el odpNumber del pedido comercial (customer_orders).")
    public record DeliveryResponse(
            @Schema(example = "del-uuid-001") String orderDeliveryId,
            @Schema(
                    description = "Número visible de la entrega (order_deliveries.delivery_number = ODP-{n}). "
                            + "Secuencia distinta de customer_orders.odp_number.",
                    example = "ODP-42"
            ) String deliveryNumber,
            @Schema(example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd") String productionOrderId,
            @Schema(allowableValues = {"entrega", "reversion"}, example = "entrega") String movementType,
            @Schema(allowableValues = {"parcial", "total"}, example = "parcial") String deliveryType,
            @Schema(description = "ID de la entrega original si movementType=reversion") String reversedDeliveryId,
            @Schema(example = "500") int quantityDelivered,
            @Schema(example = "1200.00") BigDecimal unitPrice,
            @Schema(example = "600000.00") BigDecimal totalValue,
            @Schema(example = "800") int availableBefore,
            @Schema(example = "seller-seed-001") String sellerId,
            @Schema(example = "2026-09-05T14:30:00") String deliveredAt,
            @Schema(description = "Usuario autenticado que registró la entrega", example = "operator-seed-003")
            String deliveredBy,
            String notes
    ) {
        public static DeliveryResponse from(OrderDelivery delivery) {
            DeliveryMovementType movement = delivery.getMovementType() == null
                    ? DeliveryMovementType.ENTREGA
                    : delivery.getMovementType();
            return new DeliveryResponse(
                    delivery.getOrderDeliveryId(),
                    delivery.getDeliveryNumber(),
                    delivery.getProductionOrderId(),
                    movement.getDbValue(),
                    delivery.getDeliveryType().getDbValue(),
                    delivery.getReversedDeliveryId(),
                    delivery.getQuantityDelivered(),
                    delivery.getUnitPrice(),
                    delivery.getTotalValue(),
                    delivery.getAvailableBefore(),
                    delivery.getSellerId(),
                    delivery.getDeliveredAt() == null ? null : delivery.getDeliveredAt().toString(),
                    delivery.getDeliveredBy(),
                    delivery.getNotes()
            );
        }
    }

    @Schema(name = "OrderCreatePaymentResponse", description = "Resultado de registrar un abono o reversión. "
            + "accountsReceivable trae cxcNumber, abonosNumber (ABN-n), lastPaymentNumber/lastPaymentAt y totales recalculados. "
            + "payment.paymentNumber (ABN-n) ≠ accountsReceivable.abonosNumber (ABN-n).")
    public record CreatePaymentResponse(
            PaymentResponse payment,
            AccountsReceivableResponse accountsReceivable
    ) {
        public static CreatePaymentResponse from(CreateOrderPaymentUseCase.CreatePaymentResult result) {
            return new CreatePaymentResponse(
                    PaymentResponse.from(result.payment()),
                    AccountsReceivableResponse.from(result.accountsReceivable(), result.odpNumber())
            );
        }
    }

    @Schema(name = "OrderPaymentResponse", description = "Liquidación append-only (abono/anticipo/retencion/reversion) "
            + "con paymentNumber=ABN-{n}. Distinto de abonosNumber (ABN-{n}) del agregado accounts_receivable.")
    public record PaymentResponse(
            @Schema(example = "pay-uuid-001") String orderPaymentId,
            @Schema(
                    description = "Número visible del movimiento (ABN-{n}). No es abonosNumber (ABN-{n}) del agregado.",
                    example = "ABN-15"
            ) String paymentNumber,
            @Schema(example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd") String productionOrderId,
            @Schema(allowableValues = {"abono", "anticipo", "retencion", "reversion"}, example = "abono") String paymentType,
            @Schema(example = "200000.00") BigDecimal amount,
            @Schema(allowableValues = {"efectivo", "transferencia", "cheque", "tarjeta", "otro", "retencion"}, example = "transferencia")
            String paymentMethod,
            @Schema(example = "COMP-00123") String reference,
            @Schema(description = "ID del movimiento original si paymentType=reversion") String reversedPaymentId,
            @Schema(allowableValues = {"retefuente", "reteiva", "reteica", "otro"}) String withholdingType,
            @Schema(example = "1000000.00") BigDecimal withholdingBase,
            @Schema(example = "2.5000") BigDecimal withholdingRate,
            @Schema(example = "CERT-2026-001") String certificateRef,
            @Schema(description = "Reserva FE; nullable") String invoiceId,
            @Schema(example = "2026-09-05T16:00:00") String paidAt,
            @Schema(description = "Usuario autenticado que registró el movimiento", example = "operator-seed-003")
            String registeredBy,
            String notes
    ) {
        public static PaymentResponse from(OrderPayment payment) {
            return new PaymentResponse(
                    payment.getOrderPaymentId(),
                    payment.getPaymentNumber(),
                    payment.getProductionOrderId(),
                    payment.getPaymentType().getDbValue(),
                    payment.getAmount(),
                    payment.getPaymentMethod().getDbValue(),
                    payment.getReference(),
                    payment.getReversedPaymentId(),
                    payment.getWithholdingType() == null ? null : payment.getWithholdingType().getDbValue(),
                    payment.getWithholdingBase(),
                    payment.getWithholdingRate(),
                    payment.getCertificateRef(),
                    payment.getInvoiceId(),
                    payment.getPaidAt() == null ? null : payment.getPaidAt().toString(),
                    payment.getRegisteredBy(),
                    payment.getNotes()
            );
        }
    }

    @Schema(name = "AccountsReceivableItemResponse", description = "Fila del dashboard CxC/Abonos (1 fila = 1 OP). "
            + "cxcNumber = id CxC; abonosNumber = id agregado Abonos (ABN-n ≠ paymentNumber del detalle); "
            + "totalOwed = totalToCharge OP; totalRemaining = totalOwed − totalPaid (puede ser negativo). "
            + "Unidades entregadas = cartera CxC por entregas. odpNumber = pedido; lastPaymentNumber = último movimiento.")
    public record AccountsReceivableItemResponse(
            @Schema(description = "UUID de la CxC", example = "ar-uuid-001") String accountsReceivableId,
            @Schema(description = "Número visible CXC-{n}; id de negocio del agregado CxC", example = "CXC-7") String cxcNumber,
            @Schema(
                    description = "Id de negocio del agregado de Abonos (ABN-{n}). Inmutable tras el primer INSERT del trigger. "
                            + "Misma familia ABN- que paymentNumber; valor distinto (secuencia compartida). ≠ lastPaymentNumber.",
                    example = "ABN-7"
            )
            String abonosNumber,
            @Schema(example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd") String productionOrderId,
            @Schema(example = "OP-142") String orderNumber,
            @Schema(
                    description = "Nº pedido comercial (customer_orders.odp_number = ODP-{n}). Asociado 1:1; no es abonosNumber.",
                    example = "ODP-12"
            )
            String odpNumber,
            String clientId,
            @Schema(example = "Distribuciones ACME") String clientName,
            @Schema(example = "1000") int totalUnits,
            @Schema(example = "500") int deliveredUnits,
            @Schema(example = "500") int pendingUnits,
            @Schema(description = "totalToCharge de la OP (base Abonos)", example = "1500000.00") BigDecimal totalOwed,
            @Schema(example = "300000.00") BigDecimal totalPaid,
            @Schema(description = "totalOwed − totalPaid; puede ser negativo", example = "1200000.00") BigDecimal totalRemaining,
            @Schema(example = "150000.00") BigDecimal totalCashPaid,
            @Schema(example = "50000.00") BigDecimal totalWithheld,
            @Schema(example = "0.00") BigDecimal totalAdvancePaid,
            @Schema(allowableValues = {"pendiente", "parcial", "pagado", "anulado"}, example = "parcial") String status,
            @Schema(
                    description = "Fecha/hora de la primera entrega que abrió la CxC (no se reescribe con entregas posteriores)",
                    example = "2026-09-05T14:30:00"
            )
            String openedAt,
            @Schema(example = "2026-10-05") String dueDate,
            @Schema(example = "30") int paymentTermDays,
            @Schema(allowableValues = {"al_dia", "por_vencer", "vencida", "sin_vencimiento", "anulada"}, example = "vencida")
            String collectionStatus,
            @Schema(example = "12") int daysOverdue,
            @Schema(allowableValues = {"current", "1-30", "31-60", "61-90", "90+"}, example = "1-30")
            String agingBucket,
            @Schema(example = "true") boolean overdue,
            @Schema(example = "false") boolean dueSoon,
            @Schema(
                    description = "Último deliveryNumber (ODP-{n} de order_deliveries) de la OP; null si no hay entregas. "
                            + "No es customer_orders.odpNumber.",
                    example = "ODP-42"
            )
            String lastDeliveryNumber,
            @Schema(
                    description = "deliveredAt de la última entrega (cambia en cada entrega nueva). Distinto de openedAt.",
                    example = "2026-09-05T14:30:00"
            )
            String lastDeliveryAt,
            @Schema(
                    description = "Último paymentNumber (ABN-{n}) vigente de la OP. Null si no hay liquidaciones netas. "
                            + "Análogo a lastDeliveryNumber; no es abonosNumber (ABN-{n}) ni odpNumber ni cxcNumber.",
                    example = "ABN-4"
            )
            String lastPaymentNumber,
            @Schema(
                    description = "paidAt del último abono vigente; null si no hay liquidaciones netas",
                    example = "2026-09-05T16:00:00"
            )
            String lastPaymentAt
    ) {
        public static AccountsReceivableItemResponse from(ListAccountsReceivableUseCase.AccountsReceivableRow row) {
            return new AccountsReceivableItemResponse(
                    row.accountsReceivableId(),
                    row.cxcNumber(),
                    row.abonosNumber(),
                    row.productionOrderId(),
                    row.orderNumber(),
                    row.odpNumber(),
                    row.clientId(),
                    row.clientName(),
                    row.totalUnits(),
                    row.deliveredUnits(),
                    row.pendingUnits(),
                    row.totalOwed(),
                    row.totalPaid(),
                    row.totalRemaining(),
                    row.totalCashPaid(),
                    row.totalWithheld(),
                    row.totalAdvancePaid(),
                    row.status(),
                    row.openedAt() == null ? null : row.openedAt().toString(),
                    row.dueDate() == null ? null : row.dueDate().toString(),
                    row.paymentTermDays(),
                    row.collectionStatus(),
                    row.daysOverdue(),
                    row.agingBucket(),
                    row.overdue(),
                    row.dueSoon(),
                    row.lastDeliveryNumber(),
                    row.lastDeliveryAt() == null ? null : row.lastDeliveryAt().toString(),
                    row.lastPaymentNumber(),
                    row.lastPaymentAt() == null ? null : row.lastPaymentAt().toString()
            );
        }
    }

    @Schema(name = "AccountsReceivableDetailResponse", description = "Detalle CxC/Abonos: summary (cxcNumber + abonosNumber + odpNumber + lastPaymentNumber) "
            + "+ historial entregas (deliveryNumber) + historial pagos (paymentNumber ABN). "
            + "Invariante: abonosNumber != payment.paymentNumber para todo pago del detalle "
            + "(ambos ABN-; valores distintos por secuencia compartida).")
    public record AccountsReceivableDetailResponse(
            AccountsReceivableItemResponse summary,
            List<DeliveryResponse> deliveries,
            List<PaymentResponse> payments
    ) {
    }

    @Schema(name = "ClientAccountsReceivableResponse", description = "Cartera consolidada de un cliente. "
            + "Cada OP incluye cxcNumber y abonosNumber (ABN-n ≠ paymentNumber del detalle).")
    public record ClientAccountsReceivableResponse(
            @Schema(example = "client-seed-001")
            String clientId,
            @Schema(example = "Distribuciones ACME")
            String clientName,
            List<ClientOrderAccountsReceivableResponse> orders,
            @Schema(description = "Suma totalToCharge de las OPs (base Abonos)", example = "1500000.00")
            BigDecimal totalOwed,
            @Schema(example = "300000.00")
            BigDecimal totalPaid,
            @Schema(description = "Suma de saldos Abonos (puede incluir negativos)", example = "1200000.00")
            BigDecimal totalRemaining
    ) {
        public static ClientAccountsReceivableResponse from(GetClientAccountsReceivableUseCase.ClientAccountsReceivable summary) {
            return new ClientAccountsReceivableResponse(
                    summary.clientId(),
                    summary.clientName(),
                    summary.orders().stream().map(ClientOrderAccountsReceivableResponse::from).toList(),
                    summary.totalOwed(),
                    summary.totalPaid(),
                    summary.totalRemaining()
            );
        }
    }

    @Schema(name = "ClientOrderAccountsReceivableResponse", description = "Cartera de una OP del cliente. "
            + "cxcNumber = CxC (CXC-n); abonosNumber = id agregado Abonos (ABN-n ≠ paymentNumber del detalle).")
    public record ClientOrderAccountsReceivableResponse(
            @Schema(example = "ar-uuid-001") String accountsReceivableId,
            @Schema(example = "CXC-7") String cxcNumber,
            @Schema(
                    description = "Id de negocio del agregado de Abonos (ABN-{n}). Inmutable. Distinto de paymentNumber (ABN-{n}).",
                    example = "ABN-7"
            ) String abonosNumber,
            @Schema(example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd") String productionOrderId,
            @Schema(example = "OP-142") String orderNumber,
            @Schema(example = "1000") int totalUnits,
            @Schema(example = "500") int deliveredUnits,
            @Schema(example = "500") int pendingUnits,
            @Schema(description = "totalToCharge de la OP", example = "1500000.00") BigDecimal totalOwed,
            @Schema(example = "300000.00") BigDecimal totalPaid,
            @Schema(description = "totalOwed − totalPaid", example = "1200000.00") BigDecimal totalRemaining,
            @Schema(allowableValues = {"pendiente", "parcial", "pagado", "anulado"}, example = "parcial") String status
    ) {
        private static ClientOrderAccountsReceivableResponse from(GetClientAccountsReceivableUseCase.OrderAccountsReceivable summary) {
            return new ClientOrderAccountsReceivableResponse(
                    summary.accountsReceivableId(),
                    summary.cxcNumber(),
                    summary.abonosNumber(),
                    summary.productionOrderId(),
                    summary.orderNumber(),
                    summary.totalUnits(),
                    summary.deliveredUnits(),
                    summary.pendingUnits(),
                    summary.totalOwed(),
                    summary.totalPaid(),
                    summary.totalRemaining(),
                    summary.status()
            );
        }
    }
}
