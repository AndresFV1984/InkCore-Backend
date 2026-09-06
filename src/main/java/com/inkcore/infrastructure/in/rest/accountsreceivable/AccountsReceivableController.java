package com.inkcore.infrastructure.in.rest.accountsreceivable;

import com.inkcore.application.order.usecase.GetArSummaryDetailUseCase;
import com.inkcore.application.order.usecase.ListArSummaryUseCase;
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
@Tag(name = "Cuentas por cobrar", description = "Dashboard y detalle de saldos por OP (solo lectura)")
@SecurityRequirement(name = "bearerAuth")
public class AccountsReceivableController {

    private final ListArSummaryUseCase listUseCase;
    private final GetArSummaryDetailUseCase detailUseCase;
    private final ApiResponseFactory responseFactory;

    public AccountsReceivableController(
            ListArSummaryUseCase listUseCase,
            GetArSummaryDetailUseCase detailUseCase,
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
            description = "SELECT sobre ar_summary (derivada por triggers). "
                    + "Filtros opcionales: status, clientId, search (orderNumber o clientName)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Dashboard CxC",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AccountsReceivableListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "DashboardCxC", value = OrderSwaggerExamples.AR_LIST_RESPONSE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<OrderResponses.ArItemResponse>>> list(
            @Parameter(description = "pendiente | parcial | pagado", example = "parcial")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filtrar por cliente")
            @RequestParam(required = false) String clientId,
            @Parameter(description = "Búsqueda por orderNumber o clientName", example = "OP-142")
            @RequestParam(required = false) String search,
            @Parameter(description = "Página 0-based", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página (máx 100)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        PageResponse<OrderResponses.ArItemResponse> data = PageResponse.from(
                listUseCase.execute(status, clientId, search, PageQuery.of(page, size), authentication),
                OrderResponses.ArItemResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getAccountsReceivableDetail",
            summary = "Detalle CxC + historial de entregas y abonos",
            description = "Resumen de ar_summary más ledgers order_deliveries y order_payments de la OP."
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
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.ArDetailResponse>> detail(
            @Parameter(description = "ID de la OP", required = true, example = OrderSwaggerExamples.ORDER_ID)
            @PathVariable String productionOrderId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        GetArSummaryDetailUseCase.ArDetail detail = detailUseCase.execute(productionOrderId, authentication);
        return responseFactory.okStandard(
                httpRequest,
                new OrderResponses.ArDetailResponse(
                        OrderResponses.ArItemResponse.from(detail.summary()),
                        detail.deliveries().stream().map(OrderResponses.DeliveryResponse::from).toList(),
                        detail.payments().stream().map(OrderResponses.PaymentResponse::from).toList()
                )
        );
    }
}
