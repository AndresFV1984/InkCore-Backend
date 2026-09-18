package com.inkcore.infrastructure.in.rest.orders;

import com.inkcore.application.order.usecase.CreateOrderDeliveryUseCase;
import com.inkcore.application.order.usecase.CreateOrderPaymentUseCase;
import com.inkcore.application.order.usecase.GetOrderAccountsReceivableUseCase;
import com.inkcore.application.order.usecase.GetOrderAvailabilityUseCase;
import com.inkcore.application.order.usecase.ListOrderDeliveriesUseCase;
import com.inkcore.application.order.usecase.ListOrderPaymentsUseCase;
import com.inkcore.application.order.usecase.ReverseOrderDeliveryUseCase;
import com.inkcore.application.order.usecase.ReverseOrderPaymentUseCase;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.OrderAvailabilitySuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderAccountsReceivableSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderCreateDeliverySuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderCreatePaymentSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderDeliveryListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderPaymentListSuccessEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/production-orders")
@Tag(
        name = "Pedidos",
        description = "Ledgers comerciales sobre OP: entregas (deliveryNumber ODP-{n}, distinto del odpNumber del pedido) "
                + "y abonos (ABN-{n}); append-only con reversión."
)
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final CreateOrderDeliveryUseCase createDeliveryUseCase;
    private final ReverseOrderDeliveryUseCase reverseDeliveryUseCase;
    private final ListOrderDeliveriesUseCase listDeliveriesUseCase;
    private final GetOrderAvailabilityUseCase availabilityUseCase;
    private final GetOrderAccountsReceivableUseCase accountsReceivableUseCase;
    private final CreateOrderPaymentUseCase createPaymentUseCase;
    private final ReverseOrderPaymentUseCase reversePaymentUseCase;
    private final ListOrderPaymentsUseCase listPaymentsUseCase;
    private final ApiResponseFactory responseFactory;

    public OrderController(
            CreateOrderDeliveryUseCase createDeliveryUseCase,
            ReverseOrderDeliveryUseCase reverseDeliveryUseCase,
            ListOrderDeliveriesUseCase listDeliveriesUseCase,
            GetOrderAvailabilityUseCase availabilityUseCase,
            GetOrderAccountsReceivableUseCase accountsReceivableUseCase,
            CreateOrderPaymentUseCase createPaymentUseCase,
            ReverseOrderPaymentUseCase reversePaymentUseCase,
            ListOrderPaymentsUseCase listPaymentsUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createDeliveryUseCase = createDeliveryUseCase;
        this.reverseDeliveryUseCase = reverseDeliveryUseCase;
        this.listDeliveriesUseCase = listDeliveriesUseCase;
        this.availabilityUseCase = availabilityUseCase;
        this.accountsReceivableUseCase = accountsReceivableUseCase;
        this.createPaymentUseCase = createPaymentUseCase;
        this.reversePaymentUseCase = reversePaymentUseCase;
        this.listPaymentsUseCase = listPaymentsUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/{productionOrderId}/deliveries")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "createOrderDelivery",
            summary = "Registrar entrega parcial o total",
            description = "Inserta en order_deliveries (append-only). Asigna deliveryNumber=ODP-{n} por empresa "
                    + "(secuencia propia de entregas; no es customer_orders.odpNumber del pedido comercial). "
                    + "totalValue = quantityDelivered × unitPrice. La disponibilidad la valida el trigger de BD; "
                    + "422 si no alcanza. Si es la primera entrega, el trigger materializa accounts_receivable con "
                    + "accountsReceivableId + cxcNumber=CXC-{n} y openedAt=deliveredAt de esa entrega. "
                    + "Entregas posteriores actualizan lastDeliveryAt pero no reescriben openedAt. "
                    + "Devuelve accounts_receivable actualizado (sin regenerar CxC)."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Entrega registrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderCreateDeliverySuccessEnvelope.class),
                    examples = @ExampleObject(name = "EntregaCreada", value = OrderSwaggerExamples.CREATE_DELIVERY_RESPONSE)
            )
    )
    @ApiResponse(
            responseCode = "422",
            description = "Cantidad superior a la disponibilidad liberada por planta",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class),
                    examples = @ExampleObject(
                            name = "DisponibilidadInsuficiente",
                            value = OrderSwaggerExamples.INSUFFICIENT_AVAILABILITY_RESPONSE
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.CreateDeliveryResponse>> createDelivery(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos de la entrega",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateDeliveryRequest.class),
                            examples = @ExampleObject(name = "EntregaParcial", value = OrderSwaggerExamples.CREATE_DELIVERY_REQUEST)
                    )
            )
            @Valid @RequestBody CreateDeliveryRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        CreateOrderDeliveryUseCase.CreateDeliveryResult result = createDeliveryUseCase.execute(
                new CreateOrderDeliveryUseCase.CreateOrderDeliveryCommand(
                        productionOrderId,
                        request.deliveryType(),
                        request.quantityDelivered(),
                        request.unitPrice(),
                        request.sellerId(),
                        request.deliveredAt(),
                        request.notes()
                ),
                authentication
        );
        return responseFactory.success(
                httpRequest,
                HttpStatus.CREATED,
                OrderResponses.CreateDeliveryResponse.from(result)
        );
    }

    @GetMapping("/{productionOrderId}/deliveries")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listOrderDeliveries",
            summary = "Listar entregas de una OP",
            description = "Historial append-only (entrega|reversion) ordenado por deliveredAt DESC. "
                    + "Cada fila incluye deliveryNumber (ODP-{n} de entrega, distinto del odpNumber del pedido)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Entregas",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderDeliveryListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "ListadoEntregas", value = OrderSwaggerExamples.DELIVERY_LIST_RESPONSE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<OrderResponses.DeliveryResponse>>> listDeliveries(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        List<OrderDelivery> deliveries = listDeliveriesUseCase.execute(productionOrderId, authentication);
        return responseFactory.okStandard(
                httpRequest,
                deliveries.stream().map(OrderResponses.DeliveryResponse::from).toList()
        );
    }

    @PostMapping("/{productionOrderId}/deliveries/{deliveryId}/reverse")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "reverseOrderDelivery",
            summary = "Anular una entrega (append-only)",
            description = "No borra la entrega: inserta movementType=reversion con nuevo ODP-{n}. "
                    + "409 si ya fue anulada. 422 si el saldo adeudado quedaría por debajo de lo abonado."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Reversión de entrega registrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderCreateDeliverySuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ReversionEntregaCreada",
                            value = OrderSwaggerExamples.REVERSE_DELIVERY_RESPONSE
                    )
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "La entrega ya fue anulada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class),
                    examples = @ExampleObject(
                            name = "EntregaYaAnulada",
                            value = OrderSwaggerExamples.REVERSE_DELIVERY_CONFLICT_RESPONSE
                    )
            )
    )
    @ApiResponse(
            responseCode = "422",
            description = "Anulación dejaría total_owed < total_paid",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class),
                    examples = @ExampleObject(
                            name = "SaldoInsuficienteParaAnular",
                            value = OrderSwaggerExamples.REVERSE_DELIVERY_BUSINESS_RULE_RESPONSE
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.CreateDeliveryResponse>> reverseDelivery(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @Parameter(description = "ID de la entrega a anular", required = true, example = OrderSwaggerExamples.DELIVERY_ID)
            @PathVariable String deliveryId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    description = "Motivo opcional de anulación",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReverseDeliveryRequest.class),
                            examples = @ExampleObject(name = "AnularEntrega", value = OrderSwaggerExamples.REVERSE_DELIVERY_REQUEST)
                    )
            )
            @Valid @RequestBody(required = false) ReverseDeliveryRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        CreateOrderDeliveryUseCase.CreateDeliveryResult result = reverseDeliveryUseCase.execute(
                productionOrderId, deliveryId, request == null ? null : request.reason(), authentication);
        return responseFactory.success(
                httpRequest,
                HttpStatus.CREATED,
                OrderResponses.CreateDeliveryResponse.from(result)
        );
    }

    @GetMapping("/{productionOrderId}/available")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getOrderAvailability",
            summary = "Disponibilidad comercial de una OP",
            description = "processed = station_order_progress.cantidad_disponible; "
                    + "delivered = accounts_receivable.delivered_units; available = max(0, processed − delivered)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Disponibilidad",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderAvailabilitySuccessEnvelope.class),
                    examples = @ExampleObject(name = "Disponibilidad", value = OrderSwaggerExamples.AVAILABILITY_RESPONSE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.AvailabilityResponse>> available(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.okStandard(
                httpRequest,
                OrderResponses.AvailabilityResponse.from(
                        availabilityUseCase.execute(productionOrderId, authentication))
        );
    }

    @GetMapping("/{productionOrderId}/accounts-receivable")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getOrderAccountsReceivable",
            summary = "Consultar cartera de una OP",
            description = "Lee accounts_receivable (accountsReceivableId + cxcNumber + openedAt + lastPaymentNumber). "
                    + "openedAt es la 1ª entrega que abrió la CxC (ISO local sin Z); null si aún no hay deuda. "
                    + "lastPaymentNumber es el último ABN-{n} vigente (null sin liquidaciones netas); no sustituye a cxcNumber. "
                    + "Si no existen entregas ni abonos, devuelve status=sin_movimientos sin persistir fila "
                    + "(accountsReceivableId/cxcNumber/openedAt/lastPaymentNumber nulos)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Estado de cartera",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderAccountsReceivableSuccessEnvelope.class),
                    examples = {
                            @ExampleObject(
                                    name = "ConMovimientos",
                                    value = OrderSwaggerExamples.ACCOUNTS_RECEIVABLE_RESPONSE
                            ),
                            @ExampleObject(
                                    name = "SinMovimientos",
                                    value = OrderSwaggerExamples.ACCOUNTS_RECEIVABLE_WITHOUT_MOVEMENTS_RESPONSE
                            )
                    }
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.AccountsReceivableResponse>> accountsReceivable(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.okStandard(
                httpRequest,
                OrderResponses.AccountsReceivableResponse.from(
                        accountsReceivableUseCase.execute(productionOrderId, authentication))
        );
    }

    @PostMapping("/{productionOrderId}/payments")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "createOrderPayment",
            summary = "Registrar liquidación (abono, anticipo o retención)",
            description = "Inserta order_payments (append-only) con paymentNumber=ABN-{n} (consecutivo por empresa). "
                    + "paymentType: abono|anticipo|retencion (default abono). "
                    + "Retención exige withholdingType y paymentMethod=retencion. "
                    + "Actualiza el agregado CxC existente (sin crear cuenta ABN): totalPaid/buckets, "
                    + "lastPaymentNumber=ABN del movimiento y lastPaymentAt=paidAt. "
                    + "Devuelve accounts_receivable completo (cxcNumber + lastPaymentNumber + totales)."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Liquidación registrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderCreatePaymentSuccessEnvelope.class),
                    examples = @ExampleObject(name = "AbonoCreado", value = OrderSwaggerExamples.CREATE_PAYMENT_RESPONSE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.CreatePaymentResponse>> createPayment(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos de la liquidación",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreatePaymentRequest.class),
                            examples = {
                                    @ExampleObject(name = "Abono", value = OrderSwaggerExamples.CREATE_PAYMENT_REQUEST),
                                    @ExampleObject(name = "Anticipo", value = OrderSwaggerExamples.CREATE_ANTICIPO_REQUEST),
                                    @ExampleObject(name = "Retencion", value = OrderSwaggerExamples.CREATE_RETENCION_REQUEST)
                            }
                    )
            )
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        CreateOrderPaymentUseCase.CreatePaymentResult result = createPaymentUseCase.execute(
                new CreateOrderPaymentUseCase.CreateOrderPaymentCommand(
                        productionOrderId,
                        request.amount(),
                        request.paymentType(),
                        request.paymentMethod(),
                        request.reference(),
                        request.withholdingType(),
                        request.withholdingBase(),
                        request.withholdingRate(),
                        request.certificateRef(),
                        request.invoiceId(),
                        request.paidAt(),
                        request.notes()
                ),
                authentication
        );
        return responseFactory.success(
                httpRequest,
                HttpStatus.CREATED,
                OrderResponses.CreatePaymentResponse.from(result)
        );
    }

    @PostMapping("/{productionOrderId}/payments/{paymentId}/reverse")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "reverseOrderPayment",
            summary = "Anular un abono (append-only)",
            description = "No borra el abono: inserta paymentType=reversion con el mismo monto y nuevo "
                    + "paymentNumber=ABN-{n}. Recalcula totales CxC y lastPaymentNumber/lastPaymentAt "
                    + "al último abono vigente (o null). 409 si ya fue anulado."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Reversión registrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderCreatePaymentSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ReversionCreada",
                            value = OrderSwaggerExamples.REVERSE_PAYMENT_RESPONSE
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.CreatePaymentResponse>> reversePayment(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @Parameter(description = "ID del abono a anular", required = true, example = OrderSwaggerExamples.PAYMENT_ID)
            @PathVariable String paymentId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    description = "Motivo opcional de anulación",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReversePaymentRequest.class),
                            examples = @ExampleObject(name = "Anular", value = OrderSwaggerExamples.REVERSE_PAYMENT_REQUEST)
                    )
            )
            @Valid @RequestBody(required = false) ReversePaymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        CreateOrderPaymentUseCase.CreatePaymentResult result = reversePaymentUseCase.execute(
                productionOrderId, paymentId, request == null ? null : request.reason(), authentication);
        return responseFactory.success(
                httpRequest,
                HttpStatus.CREATED,
                OrderResponses.CreatePaymentResponse.from(result)
        );
    }

    @GetMapping("/{productionOrderId}/payments")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listOrderPayments",
            summary = "Listar liquidaciones de una OP",
            description = "Historial de Abonos (movimientos ABN) append-only ordenado por paidAt DESC. "
                    + "Incluye abono|anticipo|retencion|reversion con paymentNumber (ABN-{n}). "
                    + "El resumen de cuenta (cxcNumber + lastPaymentNumber) está en GET accounts-receivable."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Pagos",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderPaymentListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "ListadoPagos", value = OrderSwaggerExamples.PAYMENT_LIST_RESPONSE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<OrderResponses.PaymentResponse>>> listPayments(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        List<OrderPayment> payments = listPaymentsUseCase.execute(productionOrderId, authentication);
        return responseFactory.okStandard(
                httpRequest,
                payments.stream().map(OrderResponses.PaymentResponse::from).toList()
        );
    }

    @Schema(
            name = "OrderCreateDeliveryRequest",
            description = "Payload para registrar una entrega. No enviar deliveryNumber/ODP ni cxcNumber: "
                    + "companyId, deliveredBy, totalValue, snapshots, availableBefore y números los determina el servidor. "
                    + "El deliveryNumber ODP-{n} de la entrega no es el odpNumber del pedido (customer_orders)."
    )
    public record CreateDeliveryRequest(
            @Schema(description = "parcial | total", allowableValues = {"parcial", "total"}, example = "parcial", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String deliveryType,
            @Schema(example = "500", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @Positive Integer quantityDelivered,
            @Schema(example = "1200.00", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
            @Schema(description = "Vendedor asociado opcional; debe pertenecer a la empresa", example = "seller-seed-001")
            String sellerId,
            @Schema(description = "Fecha/hora de entrega (sin Z)", example = "2026-09-05T14:30:00")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deliveredAt,
            @Schema(example = "Entrega recogida en bodega")
            String notes
    ) {
    }

    @Schema(
            name = "OrderCreatePaymentRequest",
            description = "Payload para registrar abono, anticipo o retención. No enviar paymentNumber/ABN: "
                    + "registeredBy y ABN-{n} se determinan en el servidor. paymentType default=abono."
    )
    public record CreatePaymentRequest(
            @Schema(example = "200000.00", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Schema(allowableValues = {"abono", "anticipo", "retencion"}, example = "abono")
            String paymentType,
            @Schema(allowableValues = {"efectivo", "transferencia", "cheque", "tarjeta", "otro", "retencion"},
                    example = "transferencia", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String paymentMethod,
            @Schema(example = "COMP-00123")
            String reference,
            @Schema(allowableValues = {"retefuente", "reteiva", "reteica", "otro"}, example = "retefuente")
            String withholdingType,
            @Schema(example = "1000000.00") BigDecimal withholdingBase,
            @Schema(example = "2.5") BigDecimal withholdingRate,
            @Schema(example = "CERT-2026-001") String certificateRef,
            @Schema(description = "Reserva FE (nullable)") String invoiceId,
            @Schema(description = "Fecha/hora del pago (sin Z)", example = "2026-09-05T16:00:00")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime paidAt,
            @Schema(example = "Abono inicial")
            String notes
    ) {
    }

    @Schema(name = "OrderReversePaymentRequest", description = "Motivo opcional de anulación")
    public record ReversePaymentRequest(
            @Schema(example = "Comprobante duplicado")
            String reason
    ) {
    }

    @Schema(name = "OrderReverseDeliveryRequest", description = "Motivo opcional de anulación de entrega")
    public record ReverseDeliveryRequest(
            @Schema(example = "Entrega duplicada")
            String reason
    ) {
    }
}
