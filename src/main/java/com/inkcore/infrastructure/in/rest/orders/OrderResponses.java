package com.inkcore.infrastructure.in.rest.orders;

import com.inkcore.application.order.usecase.CreateOrderDeliveryUseCase;
import com.inkcore.application.order.usecase.CreateOrderPaymentUseCase;
import com.inkcore.application.order.usecase.GetOrderAvailabilityUseCase;
import com.inkcore.application.order.usecase.ListArSummaryUseCase;
import com.inkcore.domain.order.model.ArSummary;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.model.OrderPayment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public final class OrderResponses {

    private OrderResponses() {
    }

    @Schema(name = "OrderAvailabilityResponse", description = "Disponibilidad comercial = procesado (estación) − entregado")
    public record AvailabilityResponse(
            @Schema(description = "Unidades procesadas (station_order_progress.cantidad_disponible)", example = "800")
            int processed,
            @Schema(description = "Unidades ya entregadas (ar_summary.delivered_units)", example = "500")
            int delivered,
            @Schema(description = "Unidades disponibles para nueva entrega", example = "300")
            int available
    ) {
        public static AvailabilityResponse from(GetOrderAvailabilityUseCase.Availability availability) {
            return new AvailabilityResponse(
                    availability.processed(), availability.delivered(), availability.available());
        }
    }

    @Schema(name = "OrderArSummaryResponse", description = "Snapshot de cuentas por cobrar tras un movimiento")
    public record ArSummaryResponse(
            @Schema(example = "500") int deliveredUnits,
            @Schema(example = "500") int pendingUnits,
            @Schema(example = "600000.00") BigDecimal totalOwed,
            @Schema(example = "200000.00") BigDecimal totalPaid,
            @Schema(example = "400000.00") BigDecimal totalRemaining,
            @Schema(allowableValues = {"pendiente", "parcial", "pagado"}, example = "parcial")
            String status
    ) {
        public static ArSummaryResponse from(ArSummary summary) {
            return new ArSummaryResponse(
                    summary.getDeliveredUnits(),
                    summary.getPendingUnits(),
                    summary.getTotalOwed(),
                    summary.getTotalPaid(),
                    summary.getTotalRemaining(),
                    summary.getStatus() == null ? null : summary.getStatus().getDbValue()
            );
        }
    }

    @Schema(name = "OrderCreateDeliveryResponse", description = "Resultado de registrar una entrega")
    public record CreateDeliveryResponse(
            @Schema(example = "del-uuid-001") String orderDeliveryId,
            @Schema(description = "Disponible justo antes de esta entrega (lo fija el trigger)", example = "800")
            int availableBefore,
            @Schema(example = "600000.00") BigDecimal totalValue,
            ArSummaryResponse accountsReceivable
    ) {
        public static CreateDeliveryResponse from(CreateOrderDeliveryUseCase.CreateDeliveryResult result) {
            return new CreateDeliveryResponse(
                    result.delivery().getOrderDeliveryId(),
                    result.delivery().getAvailableBefore(),
                    result.delivery().getTotalValue(),
                    ArSummaryResponse.from(result.accountsReceivable())
            );
        }
    }

    @Schema(name = "OrderDeliveryResponse", description = "Entrega comercial (ledger append-only)")
    public record DeliveryResponse(
            @Schema(example = "del-uuid-001") String orderDeliveryId,
            @Schema(example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd") String productionOrderId,
            @Schema(allowableValues = {"parcial", "total"}, example = "parcial") String deliveryType,
            @Schema(example = "500") int quantityDelivered,
            @Schema(example = "1200.00") BigDecimal unitPrice,
            @Schema(example = "600000.00") BigDecimal totalValue,
            @Schema(example = "800") int availableBefore,
            @Schema(example = "seller-seed-001") String sellerId,
            @Schema(example = "2026-09-05T14:30:00") String deliveredAt,
            @Schema(example = "operator-seed-003") String deliveredBy,
            String notes
    ) {
        public static DeliveryResponse from(OrderDelivery delivery) {
            return new DeliveryResponse(
                    delivery.getOrderDeliveryId(),
                    delivery.getProductionOrderId(),
                    delivery.getDeliveryType().getDbValue(),
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

    @Schema(name = "OrderCreatePaymentResponse", description = "Resultado de registrar un abono o reversión")
    public record CreatePaymentResponse(
            @Schema(example = "pay-uuid-001") String orderPaymentId,
            @Schema(allowableValues = {"abono", "reversion"}, example = "abono") String paymentType,
            @Schema(example = "200000.00") BigDecimal amount,
            ArSummaryResponse accountsReceivable
    ) {
        public static CreatePaymentResponse from(CreateOrderPaymentUseCase.CreatePaymentResult result) {
            return new CreatePaymentResponse(
                    result.payment().getOrderPaymentId(),
                    result.payment().getPaymentType().getDbValue(),
                    result.payment().getAmount(),
                    ArSummaryResponse.from(result.accountsReceivable())
            );
        }
    }

    @Schema(name = "OrderPaymentResponse", description = "Abono o reversión (ledger append-only)")
    public record PaymentResponse(
            @Schema(example = "pay-uuid-001") String orderPaymentId,
            @Schema(example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd") String productionOrderId,
            @Schema(allowableValues = {"abono", "reversion"}, example = "abono") String paymentType,
            @Schema(example = "200000.00") BigDecimal amount,
            @Schema(allowableValues = {"efectivo", "transferencia", "cheque", "tarjeta", "otro"}, example = "transferencia")
            String paymentMethod,
            @Schema(example = "COMP-00123") String reference,
            String reversedPaymentId,
            @Schema(example = "2026-09-05T16:00:00") String paidAt,
            @Schema(example = "operator-seed-003") String registeredBy,
            String notes
    ) {
        public static PaymentResponse from(OrderPayment payment) {
            return new PaymentResponse(
                    payment.getOrderPaymentId(),
                    payment.getProductionOrderId(),
                    payment.getPaymentType().getDbValue(),
                    payment.getAmount(),
                    payment.getPaymentMethod().getDbValue(),
                    payment.getReference(),
                    payment.getReversedPaymentId(),
                    payment.getPaidAt() == null ? null : payment.getPaidAt().toString(),
                    payment.getRegisteredBy(),
                    payment.getNotes()
            );
        }
    }

    @Schema(name = "AccountsReceivableItemResponse", description = "Fila del dashboard de cuentas por cobrar")
    public record ArItemResponse(
            @Schema(example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd") String productionOrderId,
            @Schema(example = "OP-142") String orderNumber,
            String clientId,
            @Schema(example = "Distribuciones ACME") String clientName,
            @Schema(example = "1000") int totalUnits,
            @Schema(example = "500") int deliveredUnits,
            @Schema(example = "500") int pendingUnits,
            @Schema(example = "600000.00") BigDecimal totalOwed,
            @Schema(example = "200000.00") BigDecimal totalPaid,
            @Schema(example = "400000.00") BigDecimal totalRemaining,
            @Schema(allowableValues = {"pendiente", "parcial", "pagado"}, example = "parcial") String status,
            String lastDeliveryAt,
            String lastPaymentAt
    ) {
        public static ArItemResponse from(ListArSummaryUseCase.ArSummaryRow row) {
            return new ArItemResponse(
                    row.productionOrderId(),
                    row.orderNumber(),
                    row.clientId(),
                    row.clientName(),
                    row.totalUnits(),
                    row.deliveredUnits(),
                    row.pendingUnits(),
                    row.totalOwed(),
                    row.totalPaid(),
                    row.totalRemaining(),
                    row.status(),
                    row.lastDeliveryAt() == null ? null : row.lastDeliveryAt().toString(),
                    row.lastPaymentAt() == null ? null : row.lastPaymentAt().toString()
            );
        }
    }

    @Schema(name = "AccountsReceivableDetailResponse", description = "Detalle CxC con historiales")
    public record ArDetailResponse(
            ArItemResponse summary,
            List<DeliveryResponse> deliveries,
            List<PaymentResponse> payments
    ) {
    }
}
