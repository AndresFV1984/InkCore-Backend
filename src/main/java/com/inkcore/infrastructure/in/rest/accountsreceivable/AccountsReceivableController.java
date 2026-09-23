package com.inkcore.infrastructure.in.rest.accountsreceivable;

import com.inkcore.application.order.usecase.GetAccountsReceivableDetailUseCase;
import com.inkcore.application.order.usecase.ListAccountsReceivableUseCase;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.AccountsReceivableDetailSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.AccountsReceivableListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import com.inkcore.infrastructure.in.rest.orders.OrderSwaggerExamples;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts-receivable")
@Tag(
        name = "Cuentas por cobrar",
        description = "Dashboard/detalle CxC y Abonos (1:1 por OP). "
                + "cxcNumber (CXC-n), abonosNumber (ABN-n, id agregado inmutable ≠ paymentNumber; secuencia ABN compartida), "
                + "odpNumber (pedido), lastPaymentNumber (último movimiento). Solo lectura; anulación implícita. Sin DELETE."
)
@SecurityRequirement(name = "bearerAuth")
public class AccountsReceivableController {

    private final ListAccountsReceivableUseCase listUseCase;
    private final GetAccountsReceivableDetailUseCase detailUseCase;
    private final ApiResponseFactory responseFactory;

    public AccountsReceivableController(
            ListAccountsReceivableUseCase listUseCase,
            GetAccountsReceivableDetailUseCase detailUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.listUseCase = listUseCase;
        this.detailUseCase = detailUseCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listAccountsReceivable",
            summary = "Listado paginado de cuentas por cobrar",
            description = "SELECT sobre accounts_receivable (derivada por triggers). Cada ítem incluye accountsReceivableId, "
                    + "cxcNumber (CXC-{n}), openedAt (deliveredAt de la 1ª entrega; no cambia con entregas posteriores), "
                    + "dueDate, collectionStatus/agingBucket para alertas, productionOrderId y orderNumber (OP-{n}). "
                    + "También abonosNumber (ABN-{n}, id del agregado de Abonos; distinto de paymentNumber ABN-{n}), "
                    + "odpNumber (pedido), lastPaymentNumber/lastPaymentAt. "
                    + "Filtros: status, clientId, overdueOnly, dueSoonOnly, withBalance, "
                    + "search (orderNumber, odpNumber, clientName, cxcNumber, abonosNumber, lastDeliveryNumber, lastPaymentNumber). "
                    + "lastDeliveryAt es la última entrega; openedAt es la primera. "
                    + "No hay DELETE ni void explícito: la CxC queda anulado/sin_movimientos cuando "
                    + "entregas y abonos netos llegan a cero tras reversiones."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Dashboard CxC / Abonos",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AccountsReceivableListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "DashboardCxC", value = OrderSwaggerExamples.AR_LIST_RESPONSE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<OrderResponses.AccountsReceivableItemResponse>>> list(
            @Parameter(description = "pendiente | parcial | pagado | anulado", example = "parcial")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filtrar por cliente")
            @RequestParam(required = false) String clientId,
            @Parameter(
                    description = "Búsqueda por orderNumber, odpNumber, clientName, cxcNumber, abonosNumber, lastDeliveryNumber o lastPaymentNumber",
                    example = "ABN-7"
            )
            @RequestParam(required = false) String search,
            @Parameter(description = "Solo CxC vencidas con saldo > 0", example = "true")
            @RequestParam(required = false) Boolean overdueOnly,
            @Parameter(description = "Solo CxC por vencer (≤7 días) con saldo > 0", example = "true")
            @RequestParam(required = false) Boolean dueSoonOnly,
            @Parameter(description = "Solo CxC/OP con saldo a liquidar (totalRemaining > 0)", example = "true")
            @RequestParam(required = false) Boolean withBalance,
            @Parameter(description = "Página 0-based", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página (máx 100)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        PageResponse<OrderResponses.AccountsReceivableItemResponse> data = PageResponse.from(
                listUseCase.execute(
                        status, clientId, search, overdueOnly, dueSoonOnly, withBalance,
                        PageQuery.of(page, size), authentication),
                OrderResponses.AccountsReceivableItemResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getAccountsReceivableDetail",
            summary = "Detalle CxC + historial de entregas y abonos",
            description = "Resumen (cxcNumber + abonosNumber ABN-n + odpNumber + lastPaymentNumber) más ledgers "
                    + "order_deliveries y order_payments (paymentNumber ABN-n). "
                    + "Invariante: abonosNumber != cualquier paymentNumber del detalle. "
                    + "Solo lectura; no existe endpoint DELETE/void de CxC."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Detalle CxC",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AccountsReceivableDetailSuccessEnvelope.class),
                    examples = @ExampleObject(name = "DetalleCxC", value = OrderSwaggerExamples.AR_DETAIL_RESPONSE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.AccountsReceivableDetailResponse>> detail(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        GetAccountsReceivableDetailUseCase.AccountsReceivableDetail detail = detailUseCase.execute(productionOrderId, authentication);
        return responseFactory.okStandard(
                httpRequest,
                new OrderResponses.AccountsReceivableDetailResponse(
                        OrderResponses.AccountsReceivableItemResponse.from(detail.summary()),
                        detail.deliveries().stream().map(OrderResponses.DeliveryResponse::from).toList(),
                        detail.payments().stream().map(OrderResponses.PaymentResponse::from).toList()
                )
        );
    }
}
