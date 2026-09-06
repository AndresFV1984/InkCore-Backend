package com.inkcore.infrastructure.in.rest.station;

import com.inkcore.application.station.usecase.GetStationBitacoraUseCase;
import com.inkcore.application.station.usecase.GetStationLaborSettlementUseCase;
import com.inkcore.application.station.usecase.GetStationProcessTimelineUseCase;
import com.inkcore.application.station.usecase.GetStationTraceReportUseCase;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.StationBitacoraSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.StationLaborSettlementSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.StationTimelineListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.StationTraceReportSuccessEnvelope;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/station")
@Tag(name = "Estación", description = "Bitácora operativa de planta, inbox y sesiones de operario")
@SecurityRequirement(name = "bearerAuth")
public class StationReportController {

    private final GetStationBitacoraUseCase bitacoraUseCase;
    private final GetStationProcessTimelineUseCase timelineUseCase;
    private final GetStationLaborSettlementUseCase laborSettlementUseCase;
    private final GetStationTraceReportUseCase traceReportUseCase;
    private final ApiResponseFactory responseFactory;

    public StationReportController(
            GetStationBitacoraUseCase bitacoraUseCase,
            GetStationProcessTimelineUseCase timelineUseCase,
            GetStationLaborSettlementUseCase laborSettlementUseCase,
            GetStationTraceReportUseCase traceReportUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.bitacoraUseCase = bitacoraUseCase;
        this.timelineUseCase = timelineUseCase;
        this.laborSettlementUseCase = laborSettlementUseCase;
        this.traceReportUseCase = traceReportUseCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/orders/{productionOrderId}/processes/{processKey}/bitacora")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getStationBitacora",
            summary = "Bitácora resumida de un proceso/ítem",
            description = "Tiempos laborales y pausas del proceso (globales). Solo `entries` se pagina."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Bitácora del proceso",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationBitacoraSuccessEnvelope.class),
                    examples = @ExampleObject(name = "Bitacora", value = StationSwaggerExamples.BITACORA)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.BitacoraResponse>> bitacora(
            @Parameter(description = "ID de la OP", required = true, example = "po-uuid-001")
            @PathVariable String productionOrderId,
            @Parameter(description = "Clave de proceso", required = true, example = "terminado:rec-barniz-001")
            @PathVariable String processKey,
            @Parameter(description = "Número de página de entries (0-based)", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página de entries (máx 100)", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @Parameter(description = "Filtrar por ítem de catálogo (terminados/acabados)")
            @RequestParam(required = false) String catalogItemId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(
                httpRequest,
                HttpStatus.OK,
                StationResponses.BitacoraResponse.from(
                        bitacoraUseCase.execute(
                                productionOrderId,
                                processKey,
                                catalogItemId,
                                PageQuery.of(page, size),
                                authentication))
        );
    }

    @GetMapping("/orders/{productionOrderId}/processes/{processKey}/timeline")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getStationProcessTimeline",
            summary = "Timeline rico paginado de un proceso/ítem",
            description = "Todos los tipos de evento de la operación, paginados en BD, ordenados por occurredAt DESC. "
                    + "Para Avance de producción (detalle). Preferir sobre /station/events + fetchAllPages."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Timeline paginado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationTimelineListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "Timeline", value = StationSwaggerExamples.TIMELINE_PAGE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<StationResponses.EventResponse>>> timeline(
            @Parameter(description = "ID de la OP", required = true, example = "po-uuid-001")
            @PathVariable String productionOrderId,
            @Parameter(description = "Clave de proceso", required = true, example = "terminado:rec-barniz-001")
            @PathVariable String processKey,
            @Parameter(description = "Página (0-based)", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página (máx 100)", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @Parameter(description = "Filtrar por ítem de catálogo")
            @RequestParam(required = false) String catalogItemId,
            @Parameter(description = "Filtrar por operario")
            @RequestParam(required = false) String userId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        PageResponse<StationResponses.EventResponse> data = PageResponse.from(
                timelineUseCase.execute(
                        productionOrderId,
                        processKey,
                        catalogItemId,
                        userId,
                        PageQuery.of(page, size),
                        authentication),
                StationResponses.EventResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/labor-settlement")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getStationLaborSettlement",
            summary = "Liquidación de tiempos laborados del operario",
            description = "Consolida intervalos de jornada, pausas y tiempo neto en el rango indicado."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Liquidación laboral",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationLaborSettlementSuccessEnvelope.class),
                    examples = @ExampleObject(name = "Liquidacion", value = StationSwaggerExamples.LABOR_SETTLEMENT)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<GetStationLaborSettlementUseCase.LaborSettlement>> laborSettlement(
            @Parameter(description = "Operario; por defecto el del JWT")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Fecha inicial inclusive", example = "2026-09-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha final inclusive", example = "2026-09-07")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(
                httpRequest,
                HttpStatus.OK,
                laborSettlementUseCase.execute(userId, from, to, authentication)
        );
    }

    @GetMapping("/trace-report")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getStationTraceReport",
            summary = "Reporte de trazabilidad por ítem de catálogo",
            description = "Filas de avance por proceso. Con includeTimeline=false omite timeline[] (lista liviana); "
                    + "el detalle de bitácora rica va a GET .../timeline."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Reporte de trazabilidad",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationTraceReportSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<GetStationTraceReportUseCase.TraceReport>> traceReport(
            @Parameter(description = "Filtrar por operario") @RequestParam(required = false) String userId,
            @Parameter(description = "Desde", example = "2026-09-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Hasta", example = "2026-09-07") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Filtrar por OP") @RequestParam(required = false) String productionOrderId,
            @Parameter(description = "Filtrar por cliente") @RequestParam(required = false) String clientId,
            @Parameter(description = "Filtrar por proceso") @RequestParam(required = false) String processKey,
            @Parameter(description = "Incluir timeline[] embebido por fila (default true por compat)", example = "false")
            @RequestParam(required = false, defaultValue = "true") Boolean includeTimeline,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(
                httpRequest,
                HttpStatus.OK,
                traceReportUseCase.execute(
                        userId, from, to, productionOrderId, clientId, processKey, includeTimeline, authentication)
        );
    }
}
