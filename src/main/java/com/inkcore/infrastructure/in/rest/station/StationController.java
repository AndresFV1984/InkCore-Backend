package com.inkcore.infrastructure.in.rest.station;

import com.inkcore.application.station.usecase.AdminCloseOrderStagesUseCase;
import com.inkcore.application.station.usecase.CompleteStationOrderIfReadyUseCase;
import com.inkcore.application.station.usecase.GetStationActiveSessionUseCase;
import com.inkcore.application.station.usecase.GetStationOrderDetailUseCase;
import com.inkcore.application.station.usecase.ListStationInboxUseCase;
import com.inkcore.application.station.usecase.ListStationOrderProcessesUseCase;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.StationActiveSessionSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.StationAdminCloseOrderStagesSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.StationCompleteOrderIfReadySuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.StationInboxListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.StationOrderDetailSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.StationProcessListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/station")
@Tag(
        name = "Estación",
        description = "Bitácora de planta: inbox, eventos (inicio/pausa/avance), intervalos y reportes. "
                + "complete-if-ready → COMPLETED; admin-close-stages (solo ADMIN). "
                + "Roles OPERADOR|ADMINISTRADOR. occurredAt sin zona (sin Z)."
)
@SecurityRequirement(name = "bearerAuth")
public class StationController {

    private final ListStationInboxUseCase inboxUseCase;
    private final GetStationOrderDetailUseCase orderDetailUseCase;
    private final ListStationOrderProcessesUseCase processesUseCase;
    private final GetStationActiveSessionUseCase activeSessionUseCase;
    private final AdminCloseOrderStagesUseCase adminCloseOrderStagesUseCase;
    private final CompleteStationOrderIfReadyUseCase completeOrderIfReadyUseCase;
    private final ApiResponseFactory responseFactory;

    public StationController(
            ListStationInboxUseCase inboxUseCase,
            GetStationOrderDetailUseCase orderDetailUseCase,
            ListStationOrderProcessesUseCase processesUseCase,
            GetStationActiveSessionUseCase activeSessionUseCase,
            AdminCloseOrderStagesUseCase adminCloseOrderStagesUseCase,
            CompleteStationOrderIfReadyUseCase completeOrderIfReadyUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.inboxUseCase = inboxUseCase;
        this.orderDetailUseCase = orderDetailUseCase;
        this.processesUseCase = processesUseCase;
        this.activeSessionUseCase = activeSessionUseCase;
        this.adminCloseOrderStagesUseCase = adminCloseOrderStagesUseCase;
        this.completeOrderIfReadyUseCase = completeOrderIfReadyUseCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/inbox")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listStationInbox",
            summary = "Inbox de órdenes asignadas al operario autenticado",
            description = "Lista paginada de OP asignadas al operario del JWT, con filtros opcionales."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Inbox paginado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationInboxListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "Inbox", value = StationSwaggerExamples.INBOX_PAGE)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<StationResponses.InboxItemResponse>>> inbox(
            @Parameter(description = "Filtrar por estado de producción", example = "En Proceso")
            @RequestParam(required = false) String productionStatus,
            @Parameter(description = "Filtrar por cliente")
            @RequestParam(required = false) String clientId,
            @Parameter(description = "Estado de procesos asignados", example = "all")
            @RequestParam(required = false, defaultValue = "all") String processStatus,
            @Parameter(description = "Número de página (0-based)", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        PageResponse<StationResponses.InboxItemResponse> data = PageResponse.from(
                inboxUseCase.execute(productionStatus, clientId, processStatus, PageQuery.of(page, size), authentication),
                StationResponses.InboxItemResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/orders/{productionOrderId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getStationOrderDetail",
            summary = "Detalle de estación para una OP",
            description = "Contexto operativo de la OP: operarios, terminados y permisos de ejecución."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Detalle de OP",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationOrderDetailSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.OrderDetailResponse>> getOrder(
            @Parameter(description = "ID de la orden de producción", required = true, example = "po-uuid-001")
            @PathVariable String productionOrderId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(
                httpRequest,
                HttpStatus.OK,
                StationResponses.OrderDetailResponse.from(orderDetailUseCase.execute(productionOrderId, authentication))
        );
    }

    @PostMapping("/orders/{productionOrderId}/complete-if-ready")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "completeStationOrderIfReady",
            summary = "Finalizar OP si todas las etapas están cumplidas",
            description = """
                    Si el cupo de cada etapa real de la OP ya está procesado o entregado,
                    pasa el estado a COMPLETED (Finalizada). Idempotente: si ya estaba COMPLETED,
                    completed=true y status=COMPLETED.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Evaluación de cierre",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationCompleteOrderIfReadySuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "OpFinalizada",
                            value = StationSwaggerExamples.COMPLETE_IF_READY_RESPONSE
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.CompleteStationOrderIfReadyResponse>> completeIfReady(
            @Parameter(description = "ID de la orden de producción", required = true, example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd")
            @PathVariable String productionOrderId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(
                httpRequest,
                HttpStatus.OK,
                StationResponses.CompleteStationOrderIfReadyResponse.from(
                        completeOrderIfReadyUseCase.execute(productionOrderId, authentication)
                )
        );
    }

    @PostMapping("/orders/{productionOrderId}/admin-close-stages")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "adminCloseOrderStages",
            summary = "Cerrar todas las etapas incompletas de una OP (administrador)",
            description = """
                    Completa el cupo pendiente (avance + entrega total) y registra fin_fase en cada
                    proceso incompleto, con observación «Cerrado por administrador: {nota}».
                    Independiente del operario asignado. Requiere nota obligatoria.
                    Si tras el cierre todas las etapas quedan cumplidas, la OP puede pasar a COMPLETED.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Etapas cerradas",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationAdminCloseOrderStagesSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "EtapasCerradas",
                            value = StationSwaggerExamples.ADMIN_CLOSE_STAGES_RESPONSE
                    )
            )
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationRequests.AdminCloseOrderStagesRequest.class),
                    examples = @ExampleObject(
                            name = "NotaAdmin",
                            value = StationSwaggerExamples.ADMIN_CLOSE_STAGES_REQUEST
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.AdminCloseOrderStagesResponse>> adminCloseStages(
            @Parameter(description = "ID de la orden de producción", required = true, example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd")
            @PathVariable String productionOrderId,
            @Valid @org.springframework.web.bind.annotation.RequestBody StationRequests.AdminCloseOrderStagesRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(
                httpRequest,
                HttpStatus.OK,
                StationResponses.AdminCloseOrderStagesResponse.from(
                        adminCloseOrderStagesUseCase.execute(productionOrderId, request.note(), authentication)
                )
        );
    }

    @GetMapping("/orders/{productionOrderId}/processes")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listStationOrderProcesses",
            summary = "Filas de proceso calculadas para una OP",
            description = "Progreso, tiempos y estado por fase/ítem de catálogo."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Lista de procesos",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationProcessListSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<StationResponses.ProcessRowResponse>>> listProcesses(
            @Parameter(description = "ID de la orden de producción", required = true, example = "po-uuid-001")
            @PathVariable String productionOrderId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        List<StationResponses.ProcessRowResponse> rows = processesUseCase.execute(productionOrderId, authentication)
                .stream()
                .map(StationResponses.ProcessRowResponse::from)
                .toList();
        return responseFactory.success(httpRequest, HttpStatus.OK, rows);
    }

    @GetMapping("/session/active")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getStationActiveSession",
            summary = "Sesión activa del operario autenticado",
            description = "Devuelve el intervalo laboral o de pausa abierto. Si no hay sesión activa, `data` es un objeto vacío."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Sesión activa o vacía",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationActiveSessionSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<Object>> activeSession(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        Object data = activeSessionUseCase.execute(authentication)
                .map(session -> (Object) StationResponses.ActiveSessionResponse.from(session))
                .orElse(Map.of());
        return responseFactory.success(httpRequest, HttpStatus.OK, data);
    }
}
