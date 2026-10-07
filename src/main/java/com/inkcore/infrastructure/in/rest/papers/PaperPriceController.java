package com.inkcore.infrastructure.in.rest.papers;

import com.inkcore.application.paper.usecase.ListPaperPriceHistoryUseCase;
import com.inkcore.application.paper.usecase.ListPaperPricesUseCase;
import com.inkcore.application.paper.usecase.PaperCommands;
import com.inkcore.application.paper.usecase.ReplacePaperPricesUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.PaperPriceHistoryListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.PaperPriceListSuccessEnvelope;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/papers/{paperId}/prices")
@Tag(name = "Papeles", description = "Catálogo (acceptsRemnants + minRemnant W/H/unit), precios, despieces, stock y remanentes.")
@SecurityRequirement(name = "bearerAuth")
public class PaperPriceController {

    private static final String PRICE_LIST_EXAMPLE = """
            {
              "headers": {
                "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "statusCode": 200,
                "code": "OK",
                "description": "OK"
              },
              "timestamp": "2026-10-06T12:00:00Z",
              "data": [
                {
                  "paperSupplierPriceId": "paper-price-seed-001",
                  "supplierId": "supplier-seed-001",
                  "sheetValue": 1500.00,
                  "packageUnit": 500,
                  "freightPerSheet": 0.00,
                  "minPurchaseSheets": 500,
                  "paymentDays": 30,
                  "deliveryDays": 5,
                  "priceDate": "2026-10-06",
                  "preferred": true,
                  "state": true,
                  "landedCostPerSheet": 1500.00
                }
              ]
            }
            """;

    private final ReplacePaperPricesUseCase replaceUseCase;
    private final ListPaperPricesUseCase listUseCase;
    private final ListPaperPriceHistoryUseCase historyUseCase;
    private final ApiResponseFactory responseFactory;

    public PaperPriceController(
            ReplacePaperPricesUseCase replaceUseCase,
            ListPaperPricesUseCase listUseCase,
            ListPaperPriceHistoryUseCase historyUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.replaceUseCase = replaceUseCase;
        this.listUseCase = listUseCase;
        this.historyUseCase = historyUseCase;
        this.responseFactory = responseFactory;
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "replacePaperPrices",
            summary = "Reemplaza la lista de precios del papel.",
            description = "Diff sobre precios vigentes: crea/actualiza/elimina según la lista. "
                    + "Solo un preferred=true por papel. packageUnit obligatorio por proveedor. "
                    + "Historial vía trigger (solo lectura). coated vive en papers, no aquí."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Precios vigentes tras el reemplazo",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperPriceListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "PreciosVigentes", value = PRICE_LIST_EXAMPLE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<PaperNestedResponses.PaperPriceResponse>>> replace(
            @Parameter(description = "Identificador del papel", required = true, example = "paper-seed-001")
            @PathVariable String paperId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Lista completa de precios a dejar vigentes",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaperRequests.ReplacePaperPricesRequest.class),
                            examples = @ExampleObject(
                                    name = "ReemplazarPrecios",
                                    value = """
                                            {
                                              "prices": [
                                                {
                                                  "supplierId": "supplier-seed-001",
                                                  "sheetValue": 1500.00,
                                                  "packageUnit": 500,
                                                  "freightPerSheet": 0.00,
                                                  "minPurchaseSheets": 500,
                                                  "paymentDays": 30,
                                                  "deliveryDays": 5,
                                                  "priceDate": "2026-10-06",
                                                  "preferred": true,
                                                  "state": true
                                                },
                                                {
                                                  "supplierId": "supplier-seed-002",
                                                  "sheetValue": 1480.00,
                                                  "packageUnit": 500,
                                                  "freightPerSheet": 0.00,
                                                  "preferred": false,
                                                  "state": true
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody PaperRequests.ReplacePaperPricesRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var command = new PaperCommands.ReplacePaperPricesCommand(
                request.prices() == null ? List.of() : request.prices().stream()
                        .map(p -> new PaperCommands.ReplacePaperPriceItem(
                                p.supplierId(), p.sheetValue(), p.packageUnit(), p.freightPerSheet(),
                                p.minPurchaseSheets(), p.paymentDays(), p.deliveryDays(),
                                p.priceDate(), p.preferred(), p.state()))
                        .toList()
        );
        var saved = replaceUseCase.execute(paperId, command, authentication);
        return responseFactory.okStandard(
                httpRequest, saved.stream().map(PaperNestedResponses.PaperPriceResponse::from).toList());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listPaperPrices",
            summary = "Lista los precios vigentes del papel.",
            description = "Incluye landedCostPerSheet (solo lectura)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Precios vigentes",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperPriceListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "PreciosVigentes", value = PRICE_LIST_EXAMPLE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<PaperNestedResponses.PaperPriceResponse>>> list(
            @Parameter(description = "Identificador del papel", required = true, example = "paper-seed-001")
            @PathVariable String paperId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var prices = listUseCase.execute(paperId, authentication);
        return responseFactory.okStandard(
                httpRequest, prices.stream().map(PaperNestedResponses.PaperPriceResponse::from).toList());
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listPaperPriceHistory",
            summary = "Historial de precios del papel (solo lectura).",
            description = "Snapshots append-only generados por trigger al crear/actualizar precios."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Historial de precios",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperPriceHistoryListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "Historial",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "OK"
                                      },
                                      "timestamp": "2026-10-06T12:00:00Z",
                                      "data": [
                                        {
                                          "paperPriceHistoryId": "paper-price-hist-001",
                                          "supplierId": "supplier-seed-001",
                                          "sheetValue": 1450.00,
                                          "packageUnit": 500,
                                          "freightPerSheet": 0.00,
                                          "minPurchaseSheets": 500,
                                          "paymentDays": 30,
                                          "deliveryDays": 5,
                                          "priceDate": "2026-09-01",
                                          "preferred": true,
                                          "state": true,
                                          "effectiveFrom": "2026-09-01T10:00:00",
                                          "changedBy": null
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<PaperNestedResponses.PaperPriceHistoryResponse>>> history(
            @Parameter(description = "Identificador del papel", required = true, example = "paper-seed-001")
            @PathVariable String paperId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var history = historyUseCase.execute(paperId, authentication);
        return responseFactory.okStandard(
                httpRequest, history.stream().map(PaperNestedResponses.PaperPriceHistoryResponse::from).toList());
    }
}
