package com.inkcore.infrastructure.in.rest.clients;

import com.inkcore.application.order.usecase.GetClientAccountsReceivableUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ClientAccountsReceivableSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.orders.OrderResponses;
import com.inkcore.infrastructure.in.rest.orders.OrderSwaggerExamples;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
@Tag(
        name = "Cuentas por cobrar",
        description = "Dashboard/detalle CxC/Abonos (CXC-{n} + abonosNumber ABN-{n} + accountsReceivableId). "
                + "Incluye openedAt (1ª entrega), dueDate/aging. Solo lectura; anulación implícita "
                + "(status anulado/sin_movimientos tras reversiones netas a cero). Sin DELETE. "
                + "abonosNumber ≠ paymentNumber del detalle (secuencia ABN compartida)."
)
@SecurityRequirement(name = "bearerAuth")
public class ClientAccountsReceivableController {

    private final GetClientAccountsReceivableUseCase useCase;
    private final ApiResponseFactory responseFactory;

    public ClientAccountsReceivableController(
            GetClientAccountsReceivableUseCase useCase,
            ApiResponseFactory responseFactory
    ) {
        this.useCase = useCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/{clientId}/accounts-receivable")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getClientAccountsReceivable",
            summary = "Consultar cartera consolidada de un cliente",
            description = "Retorna las OP con movimientos en accounts_receivable "
                    + "(cada una con accountsReceivableId + cxcNumber + abonosNumber ABN-n) "
                    + "y los totales agregados del cliente autenticado. "
                    + "abonosNumber es el id del agregado de Abonos (≠ paymentNumber ABN)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cartera consolidada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ClientAccountsReceivableSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "CarteraCliente",
                            value = OrderSwaggerExamples.CLIENT_ACCOUNTS_RECEIVABLE_RESPONSE
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<OrderResponses.ClientAccountsReceivableResponse>> getSummary(
            @Parameter(
                    description = "ID del cliente",
                    required = true,
                    example = OrderSwaggerExamples.CLIENT_ID
            )
            @PathVariable String clientId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.okStandard(
                httpRequest,
                OrderResponses.ClientAccountsReceivableResponse.from(
                        useCase.execute(clientId, authentication))
        );
    }
}
