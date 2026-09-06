package com.inkcore.infrastructure.in.rest.orders;

import com.inkcore.application.order.usecase.CreateOrderDeliveryUseCase;
import com.inkcore.application.order.usecase.CreateOrderPaymentUseCase;
import com.inkcore.application.order.usecase.GetOrderAvailabilityUseCase;
import com.inkcore.application.order.usecase.ListOrderDeliveriesUseCase;
import com.inkcore.application.order.usecase.ListOrderPaymentsUseCase;
import com.inkcore.application.order.usecase.ReverseOrderPaymentUseCase;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.OrderAvailabilitySuccessEnvelope;
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
@RequestMapping("/api/v1/orders")
@Tag(name = "Pedidos", description = "Entregas comerciales y abonos sobre órdenes de producción")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final CreateOrderDeliveryUseCase createDeliveryUseCase;
    private final ListOrderDeliveriesUseCase listDeliveriesUseCase;
    private final GetOrderAvailabilityUseCase availabilityUseCase;
    private final CreateOrderPaymentUseCase createPaymentUseCase;
    private final ReverseOrderPaymentUseCase reversePaymentUseCase;
    private final ListOrderPaymentsUseCase listPaymentsUseCase;
    private final ApiResponseFactory responseFactory;

    public OrderController(
            CreateOrderDeliveryUseCase createDeliveryUseCase,
            ListOrderDeliveriesUseCase listDeliveriesUseCase,
            GetOrderAvailabilityUseCase availabilityUseCase,
            CreateOrderPaymentUseCase createPaymentUseCase,
            ReverseOrderPaymentUseCase reversePaymentUseCase,
            ListOrderPaymentsUseCase listPaymentsUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createDeliveryUseCase = createDeliveryUseCase;
        this.listDeliveriesUseCase = listDeliveriesUseCase;
        this.availabilityUseCase = availabilityUseCase;
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
            description = "Inserta en order_deliveries (append-only). totalValue = quantityDelivered × unitPrice. "
                    + "La disponibilidad la valida el trigger de BD; 409 si no alcanza. "
                    + "deliveryType=total marca la OP como ENTREGADO. Devuelve ar_summary actualizado."
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
            description = "Historial append-only ordenado por deliveredAt DESC."
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

    @GetMapping("/{productionOrderId}/available")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getOrderAvailability",
            summary = "Disponibilidad comercial de una OP",
            description = "processed = station_order_progress.cantidad_disponible; "
                    + "delivered = ar_summary.delivered_units; available = max(0, processed − delivered)."
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

    @PostMapping("/{productionOrderId}/payments")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "createOrderPayment",
            summary = "Registrar abono sobre una OP",
            description = "Inserta order_payments con paymentType=abono (fijo). "
                    + "409 si amount supera totalRemaining. Devuelve ar_summary actualizado."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Abono registrado",
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
                    description = "Datos del abono (sin paymentType)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreatePaymentRequest.class),
                            examples = @ExampleObject(name = "Abono", value = OrderSwaggerExamples.CREATE_PAYMENT_REQUEST)
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
                        request.paymentMethod(),
                        request.reference(),
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
            description = "No borra el abono: inserta paymentType=reversion con el mismo monto. "
                    + "409 si ya fue anulado."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Reversión registrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = OrderCreatePaymentSuccessEnvelope.class)
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
                    required = true,
                    description = "Motivo de anulación",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReversePaymentRequest.class),
                            examples = @ExampleObject(name = "Anular", value = OrderSwaggerExamples.REVERSE_PAYMENT_REQUEST)
                    )
            )
            @Valid @RequestBody ReversePaymentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        CreateOrderPaymentUseCase.CreatePaymentResult result = reversePaymentUseCase.execute(
                productionOrderId, paymentId, request.reason(), authentication);
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
            summary = "Listar abonos y reversiones de una OP",
            description = "Historial append-only ordenado por paidAt DESC."
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

    @Schema(name = "OrderCreateDeliveryRequest", description = "Payload para registrar una entrega comercial")
    public record CreateDeliveryRequest(
            @Schema(description = "parcial | total", allowableValues = {"parcial", "total"}, example = "parcial", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String deliveryType,
            @Schema(example = "500", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @Positive Integer quantityDelivered,
            @Schema(example = "1200.00", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
            @Schema(description = "Vendedor opcional", example = "seller-seed-001")
            String sellerId,
            @Schema(description = "Fecha/hora de entrega (sin Z)", example = "2026-09-05T14:30:00")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deliveredAt,
            @Schema(example = "Entrega recogida en bodega")
            String notes
    ) {
    }

    @Schema(name = "OrderCreatePaymentRequest", description = "Payload para registrar un abono (paymentType fijo = abono)")
    public record CreatePaymentRequest(
            @Schema(example = "200000.00", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Schema(allowableValues = {"efectivo", "transferencia", "cheque", "tarjeta", "otro"}, example = "transferencia", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String paymentMethod,
            @Schema(example = "COMP-00123")
            String reference,
            @Schema(description = "Fecha/hora del pago (sin Z)", example = "2026-09-05T16:00:00")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime paidAt,
            @Schema(example = "Abono inicial")
            String notes
    ) {
    }

    @Schema(name = "OrderReversePaymentRequest", description = "Motivo obligatorio de anulación")
    public record ReversePaymentRequest(
            @Schema(example = "Comprobante duplicado", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String reason
    ) {
    }
}
