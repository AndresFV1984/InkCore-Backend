package com.inkcore.infrastructure.in.rest.orders;

import com.inkcore.application.order.usecase.ApplyPedidoCreditToCxcUseCase;
import com.inkcore.application.order.usecase.CreateCxcFromAdditionUseCase;
import com.inkcore.application.order.usecase.CreateNegotiatedAdditionUseCase;
import com.inkcore.application.order.usecase.CreateOrderDeliveryUseCase;
import com.inkcore.application.order.usecase.CreateOrderPaymentUseCase;
import com.inkcore.application.order.usecase.GetOrderAccountsReceivableUseCase;
import com.inkcore.application.order.usecase.GetOrderAvailabilityUseCase;
import com.inkcore.application.order.usecase.ListNegotiatedAdditionsUseCase;
import com.inkcore.application.order.usecase.ListOrderCxcDocumentsUseCase;
import com.inkcore.application.order.usecase.ListOrderDeliveriesUseCase;
import com.inkcore.application.order.usecase.ListOrderPaymentsUseCase;
import com.inkcore.application.order.usecase.ReverseNegotiatedAdditionUseCase;
import com.inkcore.application.order.usecase.UpdateNegotiatedAdditionUseCase;
import com.inkcore.application.order.usecase.ReverseOrderDeliveryUseCase;
import com.inkcore.application.order.usecase.ReverseOrderPaymentUseCase;
import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.OrderAvailabilitySuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderAccountsReceivableSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderApplyCreditToCxcSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderCreateCxcFromAdditionSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderCreateDeliverySuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderCreateNegotiatedAdditionSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderCreatePaymentSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderCxcDocumentListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderDeliveryListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.OrderNegotiatedAdditionListSuccessEnvelope;
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
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/production-orders")
@Tag(
        name = "Pedidos",
        description = "Ledgers comerciales: ENT-{n}, LIQ-{n}, ADN-{n}; documentos CxC (1 ENT/ADN → 1 CxC). "
                + "Pagos con paymentConcept + cxcDocumentId; aplicaciones y saldo a favor. "
                + "Series ODP / ENT / CXC / ABN / LIQ independientes."
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
    private final CreateNegotiatedAdditionUseCase createNegotiatedAdditionUseCase;
    private final ReverseNegotiatedAdditionUseCase reverseNegotiatedAdditionUseCase;
    private final UpdateNegotiatedAdditionUseCase updateNegotiatedAdditionUseCase;
    private final ListNegotiatedAdditionsUseCase listNegotiatedAdditionsUseCase;
    private final ListOrderCxcDocumentsUseCase listCxcDocumentsUseCase;
    private final CreateCxcFromAdditionUseCase createCxcFromAdditionUseCase;
    private final ApplyPedidoCreditToCxcUseCase applyPedidoCreditToCxcUseCase;
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
            CreateNegotiatedAdditionUseCase createNegotiatedAdditionUseCase,
            ReverseNegotiatedAdditionUseCase reverseNegotiatedAdditionUseCase,
            UpdateNegotiatedAdditionUseCase updateNegotiatedAdditionUseCase,
            ListNegotiatedAdditionsUseCase listNegotiatedAdditionsUseCase,
            ListOrderCxcDocumentsUseCase listCxcDocumentsUseCase,
            CreateCxcFromAdditionUseCase createCxcFromAdditionUseCase,
            ApplyPedidoCreditToCxcUseCase applyPedidoCreditToCxcUseCase,
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
        this.createNegotiatedAdditionUseCase = createNegotiatedAdditionUseCase;
        this.reverseNegotiatedAdditionUseCase = reverseNegotiatedAdditionUseCase;
        this.updateNegotiatedAdditionUseCase = updateNegotiatedAdditionUseCase;
        this.listNegotiatedAdditionsUseCase = listNegotiatedAdditionsUseCase;
        this.listCxcDocumentsUseCase = listCxcDocumentsUseCase;
        this.createCxcFromAdditionUseCase = createCxcFromAdditionUseCase;
        this.applyPedidoCreditToCxcUseCase = applyPedidoCreditToCxcUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/{productionOrderId}/deliveries")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "createOrderDelivery",
            summary = "Registrar entrega parcial o total",
            description = "Inserta en order_deliveries (append-only). Asigna deliveryNumber=ENT-{n} por empresa "
                    + "(secuencia ENT propia; ≠ customer_orders.odpNumber ODP; ≠ cxcNumber CXC). "
                    + "totalValue = quantityDelivered × unitPrice. La disponibilidad la valida el trigger de BD; "
                    + "422 si no alcanza. abonosNumber=ABN-{n} nace con el pedido; en la 1ª entrega el trigger "
                    + "asigna cxcNumber=CXC-{n} y openedAt=deliveredAt (si la cuenta shell aún no tenía CXC). "
                    + "Entregas posteriores actualizan lastDeliveryAt pero no reescriben openedAt ni abonosNumber. "
                    + "Devuelve accounts_receivable actualizado (sin regenerar abonosNumber)."
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
                    + "Cada fila incluye deliveryNumber ENT-{n} (≠ odpNumber ODP del pedido, ≠ cxcNumber CXC)."
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
            description = "No borra la entrega: inserta movementType=reversion con nuevo ENT-{n}. "
                    + "Recalcula unidades/saldos CxC; cxcNumber y abonosNumber (ABN) no cambian. "
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
            description = "Lee accounts_receivable (cxcNumber + abonosNumber ABN-n + odpNumber + openedAt + lastPaymentNumber). "
                    + "Abonos: totalOwed = amountToCollect (totalToCharge + ADN vigentes); "
                    + "totalRemaining = totalOwed − totalPaid (puede ser negativo). "
                    + "Sin movimientos aún, totalOwed ya refleja amountToCollect si la OP tiene costos o ADN. "
                    + "abonosNumber es el id del agregado de Abonos (ABN-n; ≠ paymentNumber LIQ-n; series separadas); "
                    + "se asigna en el primer INSERT del trigger y no se regenera. "
                    + "Si no hay movimientos, status=sin_movimientos (ids de cuenta nulos; odpNumber puede existir)."
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
            description = "Inserta order_payments (append-only) con paymentNumber=LIQ-{n} (consecutivo por empresa). "
                    + "paymentType: abono|anticipo|retencion (default abono). "
                    + "paymentConcept: entrega|extra (default entrega). "
                    + "Si hay CxC abiertas del mismo concepto, cxcDocumentId es obligatorio "
                    + "(nunca auto-asigna); excedente → saldo a favor del pedido. "
                    + "Sin CxC abiertas del concepto, todo el monto va a favor. "
                    + "Retención exige withholdingType y paymentMethod=retencion. "
                    + "Actualiza el agregado accounts_receivable (ABN) y crea order_payment_applications."
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
                        request.notes(),
                        request.paymentConcept(),
                        request.cxcDocumentId()
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
                    + "paymentNumber=LIQ-{n}. Recalcula totales CxC y lastPaymentNumber/lastPaymentAt "
                    + "al último abono vigente (o null). abonosNumber (ABN) no cambia. 409 si ya fue anulado."
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
            description = "Historial de liquidaciones (movimientos LIQ) append-only ordenado por paidAt DESC. "
                    + "Incluye abono|anticipo|retencion|reversion con paymentNumber (LIQ-{n}). "
                    + "El id del agregado de Abonos es abonosNumber (ABN-{n}) en GET accounts-receivable; "
                    + "series ABN (cuenta) y LIQ (movimiento) separadas."
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

    @GetMapping("/{productionOrderId}/cxc-documents")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listOrderCxcDocuments",
            summary = "Listar CxC abiertas del pedido",
            description = "Documentos de cartera CxC. Por defecto openOnly=true (saldo > 0). "
                    + "openOnly=false incluye pagadas (p. ej. saber si un ADN ya tiene CxC). "
                    + "Filtrar por concept=entrega|extra. "
                    + "Selector de pago: cxcDocumentId obligatorio si hay filas abiertas del mismo concepto."
    )
    @ApiResponse(
            responseCode = "200",
            description = "CxC del pedido",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderCxcDocumentListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "ListadoCxC", value = OrderSwaggerExamples.CXC_DOCUMENT_LIST_RESPONSE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<OrderResponses.CxcDocumentResponse>>> listCxcDocuments(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @Parameter(description = "entrega | extra (opcional)")
            @RequestParam(required = false) String concept,
            @Parameter(description = "true = solo saldo > 0 (default); false = todas excepto anuladas")
            @RequestParam(required = false, defaultValue = "true") boolean openOnly,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.okStandard(
                httpRequest,
                listCxcDocumentsUseCase.execute(productionOrderId, concept, openOnly, authentication)
                        .stream()
                        .map(OrderResponses.CxcDocumentResponse::from)
                        .toList()
        );
    }

    @PostMapping("/{productionOrderId}/cxc-documents/from-addition")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "createCxcFromAddition",
            summary = "Abrir CxC extra desde un ADN",
            description = "1 ADN vigente → 1 CxC concepto extra. Idempotente si ya existe CxC activa del ADN."
    )
    @ApiResponse(
            responseCode = "201",
            description = "CxC creada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderCreateCxcFromAdditionSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "CxCExtraCreada",
                            value = OrderSwaggerExamples.CREATE_CXC_FROM_ADDITION_RESPONSE
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.CreateCxcFromAdditionResponse>> createCxcFromAddition(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "ADN vigente a facturar como CxC extra",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateCxcFromAdditionRequest.class),
                            examples = @ExampleObject(
                                    name = "DesdeAdn",
                                    value = OrderSwaggerExamples.CREATE_CXC_FROM_ADDITION_REQUEST
                            )
                    )
            )
            @Valid @RequestBody CreateCxcFromAdditionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        CreateCxcFromAdditionUseCase.Result result = createCxcFromAdditionUseCase.execute(
                productionOrderId, request.additionId(), authentication);
        return responseFactory.success(
                httpRequest,
                HttpStatus.CREATED,
                OrderResponses.CreateCxcFromAdditionResponse.from(result)
        );
    }

    @PostMapping("/{productionOrderId}/cxc-documents/{cxcDocumentId}/apply-credit")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "applyPedidoCreditToCxc",
            summary = "Aplicar saldo a favor a una CxC",
            description = "Consume saldo a favor del pedido (apps sin CxC) hacia la CxC indicada. No crea LIQ nuevo."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Favor aplicado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderApplyCreditToCxcSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "FavorAplicado",
                            value = OrderSwaggerExamples.APPLY_CREDIT_TO_CXC_RESPONSE
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.ApplyCreditToCxcResponse>> applyCreditToCxc(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @Parameter(description = "ID del documento CxC", required = true,
                    example = OrderSwaggerExamples.CXC_DOCUMENT_ID)
            @PathVariable String cxcDocumentId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Monto del saldo a favor a aplicar a la CxC",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApplyCreditToCxcRequest.class),
                            examples = @ExampleObject(
                                    name = "AplicarFavor",
                                    value = OrderSwaggerExamples.APPLY_CREDIT_TO_CXC_REQUEST
                            )
                    )
            )
            @Valid @RequestBody ApplyCreditToCxcRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        ApplyPedidoCreditToCxcUseCase.Result result = applyPedidoCreditToCxcUseCase.execute(
                new ApplyPedidoCreditToCxcUseCase.Command(
                        productionOrderId, cxcDocumentId, request.amount()),
                authentication
        );
        return responseFactory.okStandard(
                httpRequest,
                OrderResponses.ApplyCreditToCxcResponse.from(result)
        );
    }

    @PostMapping("/{productionOrderId}/negotiated-additions")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "createNegotiatedAddition",
            summary = "Registrar adicional negociado",
            description = "Cambio de alcance que el cliente aprueba y paga. No modifica el costeo de planta "
                    + "(totalToCharge / PDF de cobro). Inserta order_negotiated_additions "
                    + "con additionNumber=ADN-{n}. amountToCollect = totalToCharge + suma vigente de ADN. "
                    + "Al registrar abre de una la CxC EXTRA (Adicionales) asociada al ADN. "
                    + "La lectura de Abonos usa amountToCollect como totalOwed. "
                    + "Obligatorios: concept, changeReason, amount > 0, approvedByClientName, approvalChannel."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Adicional registrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderCreateNegotiatedAdditionSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "AdicionalCreado",
                            value = OrderSwaggerExamples.CREATE_NEGOTIATED_ADDITION_RESPONSE
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.CreateNegotiatedAdditionResponse>> createNegotiatedAddition(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos del adicional negociado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateNegotiatedAdditionRequest.class),
                            examples = @ExampleObject(
                                    name = "Troquel",
                                    value = OrderSwaggerExamples.CREATE_NEGOTIATED_ADDITION_REQUEST
                            )
                    )
            )
            @Valid @RequestBody CreateNegotiatedAdditionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        CreateNegotiatedAdditionUseCase.NegotiatedAdditionResult result = createNegotiatedAdditionUseCase.execute(
                new CreateNegotiatedAdditionUseCase.CreateNegotiatedAdditionCommand(
                        productionOrderId,
                        request.concept(),
                        request.changeReason(),
                        request.amount(),
                        request.scheduleImpactDays(),
                        request.approvedByClientName(),
                        request.approvalChannel(),
                        request.approvalReference(),
                        request.approvedAt(),
                        request.notes()
                ),
                authentication
        );
        return responseFactory.success(
                httpRequest,
                HttpStatus.CREATED,
                OrderResponses.CreateNegotiatedAdditionResponse.from(result)
        );
    }

    @PutMapping("/{productionOrderId}/negotiated-additions/{additionId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateNegotiatedAddition",
            summary = "Editar un adicional negociado",
            description = "Actualiza el mismo registro ADN vigente. No crea otro número. "
                    + "409 si ya fue anulado. Recalcula amountToCollect y totalOwed de Abonos."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Adicional actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderCreateNegotiatedAdditionSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "AdicionalActualizado",
                            value = OrderSwaggerExamples.CREATE_NEGOTIATED_ADDITION_RESPONSE
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.CreateNegotiatedAdditionResponse>> updateNegotiatedAddition(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @Parameter(description = "ID del adicional", required = true, example = OrderSwaggerExamples.ADDITION_ID)
            @PathVariable String additionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos actualizados del adicional",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateNegotiatedAdditionRequest.class),
                            examples = @ExampleObject(
                                    name = "Troquel",
                                    value = OrderSwaggerExamples.CREATE_NEGOTIATED_ADDITION_REQUEST
                            )
                    )
            )
            @Valid @RequestBody CreateNegotiatedAdditionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        CreateNegotiatedAdditionUseCase.NegotiatedAdditionResult result = updateNegotiatedAdditionUseCase.execute(
                productionOrderId,
                additionId,
                new CreateNegotiatedAdditionUseCase.CreateNegotiatedAdditionCommand(
                        productionOrderId,
                        request.concept(),
                        request.changeReason(),
                        request.amount(),
                        request.scheduleImpactDays(),
                        request.approvedByClientName(),
                        request.approvalChannel(),
                        request.approvalReference(),
                        request.approvedAt(),
                        request.notes()
                ),
                authentication
        );
        return responseFactory.okStandard(
                httpRequest,
                OrderResponses.CreateNegotiatedAdditionResponse.from(result)
        );
    }

    @PostMapping("/{productionOrderId}/negotiated-additions/{additionId}/reverse")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "reverseNegotiatedAddition",
            summary = "Anular un adicional negociado",
            description = "Marca el mismo ADN como anulado (voidedAt/voidedBy). No asigna otro consecutivo. "
                    + "Si existe CxC extra del ADN sin abonos, la anula. "
                    + "422 si la CxC ya tiene abonos. 409 si ya fue anulado. "
                    + "amountToCollect y totalOwed de Abonos dejan de incluir ese monto."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Adicional anulado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderCreateNegotiatedAdditionSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "AdicionalAnulado",
                            value = OrderSwaggerExamples.REVERSE_NEGOTIATED_ADDITION_RESPONSE
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.CreateNegotiatedAdditionResponse>> reverseNegotiatedAddition(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            @Parameter(description = "ID del adicional", required = true, example = OrderSwaggerExamples.ADDITION_ID)
            @PathVariable String additionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    description = "Motivo opcional de anulación",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReverseNegotiatedAdditionRequest.class),
                            examples = @ExampleObject(
                                    name = "Anular",
                                    value = OrderSwaggerExamples.REVERSE_NEGOTIATED_ADDITION_REQUEST
                            )
                    )
            )
            @Valid @RequestBody(required = false) ReverseNegotiatedAdditionRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        CreateNegotiatedAdditionUseCase.NegotiatedAdditionResult result = reverseNegotiatedAdditionUseCase.execute(
                productionOrderId,
                additionId,
                request == null ? null : request.reason(),
                authentication
        );
        return responseFactory.success(
                httpRequest,
                HttpStatus.OK,
                OrderResponses.CreateNegotiatedAdditionResponse.from(result)
        );
    }

    @GetMapping("/{productionOrderId}/negotiated-additions")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listNegotiatedAdditions",
            summary = "Listar adicionales negociados de una OP",
            description = "Adicionales de la OP, vigentes y anulados, ordenados por approvedAt DESC. "
                    + "additionNumber=ADN-{n}. Un anulado conserva su número (voidedAt no nulo)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Adicionales",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderNegotiatedAdditionListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ListadoAdicionales",
                            value = OrderSwaggerExamples.NEGOTIATED_ADDITION_LIST_RESPONSE
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<OrderResponses.NegotiatedAdditionResponse>>> listNegotiatedAdditions(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        List<NegotiatedAddition> additions = listNegotiatedAdditionsUseCase.execute(productionOrderId, authentication);
        return responseFactory.okStandard(
                httpRequest,
                additions.stream().map(OrderResponses.NegotiatedAdditionResponse::from).toList()
        );
    }

    @Schema(
            name = "OrderCreateDeliveryRequest",
            description = "Payload para registrar una entrega. No enviar deliveryNumber ni cxcNumber: "
                    + "companyId, deliveredBy, totalValue, snapshots, availableBefore y números los determina el servidor. "
                    + "El deliveryNumber ENT-{n} es serie propia (≠ odpNumber ODP del pedido, ≠ cxcNumber CXC)."
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
                    + "registeredBy y LIQ-{n} se determinan en el servidor. paymentType default=abono. "
                    + "paymentConcept default=entrega. cxcDocumentId obligatorio si hay CxC abiertas del concepto."
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
            @Schema(description = "Reserva FE electrónica (nullable); distinto de cxcDocumentId") String invoiceId,
            @Schema(description = "Fecha/hora del pago (sin Z)", example = "2026-09-05T16:00:00")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime paidAt,
            @Schema(example = "Abono inicial")
            String notes,
            @Schema(allowableValues = {"entrega", "extra"}, example = "entrega",
                    description = "Concepto de aplicación del pago (default entrega)")
            String paymentConcept,
            @Schema(description = "CxC destino obligatoria si hay CxC abiertas del paymentConcept",
                    example = "cxc-uuid-001")
            String cxcDocumentId
    ) {
    }

    @Schema(name = "OrderCreateCxcFromAdditionRequest",
            description = "Factura un ADN vigente como CxC concepto extra.")
    public record CreateCxcFromAdditionRequest(
            @Schema(example = "adn-uuid-001", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String additionId
    ) {
    }

    @Schema(name = "OrderApplyCreditToCxcRequest",
            description = "Aplica saldo a favor del pedido a una CxC abierta.")
    public record ApplyCreditToCxcRequest(
            @Schema(example = "150000.00", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin("0.01") BigDecimal amount
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

    @Schema(
            name = "CreateNegotiatedAdditionRequest",
            description = "Cambio de alcance aprobado por el cliente. No enviar additionNumber: el servidor asigna ADN-{n}."
    )
    public record CreateNegotiatedAdditionRequest(
            @Schema(example = "Troquel adicional en la tapa", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
            @NotBlank @Size(max = 200) String concept,
            @Schema(
                    allowableValues = {"solicitud_cliente", "condicion_no_prevista", "ajuste_planta", "otro"},
                    example = "solicitud_cliente",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            @NotBlank String changeReason,
            @Schema(example = "180000.00", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Schema(description = "Días que se suman a la entrega. 0 si el plazo no cambia.", example = "0")
            @jakarta.validation.constraints.Min(0) Integer scheduleImpactDays,
            @Schema(example = "María Gómez", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150)
            @NotBlank @Size(max = 150) String approvedByClientName,
            @Schema(
                    allowableValues = {"correo", "whatsapp", "firma", "otro"},
                    example = "correo",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            @NotBlank String approvalChannel,
            @Schema(description = "Asunto, chat o documento de la aprobación", example = "Correo 19/10 asunto Troquel", maxLength = 120)
            @Size(max = 120) String approvalReference,
            @Schema(description = "Fecha/hora de aprobación del cliente (sin Z)", example = "2026-10-19T11:30:00")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime approvedAt,
            @Schema(example = "Aprobado por correo el 19/10")
            String notes
    ) {
    }

    @Schema(name = "ReverseNegotiatedAdditionRequest", description = "Motivo opcional de anulación del adicional")
    public record ReverseNegotiatedAdditionRequest(
            @Schema(example = "El cliente desistió del troquel")
            String reason
    ) {
    }
}
