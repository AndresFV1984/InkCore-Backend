package com.inkcore.infrastructure.in.rest.reports;

import com.inkcore.application.productionorder.usecase.ListProfitabilityReportUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ProfitabilityReportSuccessEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reportes", description = "Rentabilidad real de órdenes de producción")
@SecurityRequirement(name = "bearerAuth")
public class ProfitabilityReportController {

    private final ListProfitabilityReportUseCase listProfitabilityReportUseCase;
    private final ApiResponseFactory responseFactory;

    public ProfitabilityReportController(
            ListProfitabilityReportUseCase listProfitabilityReportUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.listProfitabilityReportUseCase = listProfitabilityReportUseCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/profitability")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listProfitabilityReport",
            summary = "Lista la rentabilidad real por orden.",
            description = "Órdenes con margen real, de menor a mayor, para ver qué trabajos o clientes dejan pérdida. "
                    + "companyId sale del JWT. from y to filtran production_orders.order_date. "
                    + "Cada fila incluye estimatedMermaCost, actualMermaCost y actualDesperdicioCost. "
                    + "actualWasteCost y actualTotalCost siguen incluyendo merma real y desperdicio."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Rentabilidad por orden",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProfitabilityReportSuccessEnvelope.class),
                    examples = @ExampleObject(name = "Rentabilidad", value = ProfitabilitySwaggerExamples.LIST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<ProfitabilityReportItem>>> profitability(
            @Parameter(description = "Fecha orden desde (inclusive)", required = true, example = "2026-09-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha orden hasta (inclusive)", required = true, example = "2026-09-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Filtro por cliente", example = "client-seed-001")
            @RequestParam(required = false) String clientId,
            @Parameter(description = "Filtro por vendedor", example = "seller-seed-001")
            @RequestParam(required = false) String sellerId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        List<ProfitabilityReportItem> data = listProfitabilityReportUseCase
                .execute(from, to, clientId, sellerId, authentication)
                .stream()
                .map(ProfitabilityReportItem::from)
                .toList();
        return responseFactory.success(httpRequest, HttpStatus.OK, data);
    }
}
