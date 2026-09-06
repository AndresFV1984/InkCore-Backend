package com.inkcore.infrastructure.in.rest.station;

import com.inkcore.application.station.usecase.ListStationEventsUseCase;
import com.inkcore.application.station.usecase.RegisterStationEventCommand;
import com.inkcore.application.station.usecase.RegisterStationEventUseCase;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.StationEventListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.StationEventSuccessEnvelope;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/station/events")
@Tag(name = "Estación", description = "Bitácora operativa de planta, inbox y sesiones de operario")
@SecurityRequirement(name = "bearerAuth")
public class StationEventController {

    private static final String EVENT_CONTRACT = """
            Contrato común `StationEventRequest`:
            - `phase` / `processKey` en kebab-case (`preprensa`, `corte-papel`, `impresion`, …).
            - `userId` debe estar asignado a esa fase en la OP (si se omite, usa el JWT).
            - `occurredAt` es LocalDateTime ISO **sin zona** (no enviar sufijo `Z`).
            - `actorUserId`, `clientId` y snapshots los completa el backend.
            - Roles JWT: `OPERADOR` o `ADMINISTRADOR`.
            Reglas de negocio → 422; errores de datos → 503.
            """;

    private final RegisterStationEventUseCase registerUseCase;
    private final ListStationEventsUseCase listUseCase;
    private final ApiResponseFactory responseFactory;

    public StationEventController(
            RegisterStationEventUseCase registerUseCase,
            ListStationEventsUseCase listUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.registerUseCase = registerUseCase;
        this.listUseCase = listUseCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listStationEvents",
            summary = "Listar eventos de estación con filtros",
            description = "Consulta paginada de eventos operativos por OP, operario, fase, proceso y rango de fechas."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Eventos paginados",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationEventListSuccessEnvelope.class),
                    examples = @ExampleObject(name = "ListadoEventos", value = StationSwaggerExamples.EVENT_LIST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<StationResponses.EventResponse>>> list(
            @Parameter(description = "Filtrar por OP", example = "ef658d09-bf30-43de-ba7a-edd0f14a61bd")
            @RequestParam(required = false) String productionOrderId,
            @Parameter(description = "Filtrar por operario") @RequestParam(required = false) String userId,
            @Parameter(description = "Filtrar por fase", example = "preprensa") @RequestParam(required = false) String phase,
            @Parameter(description = "Filtrar por clave de proceso", example = "preprensa") @RequestParam(required = false) String processKey,
            @Parameter(description = "Tipo de evento", example = "inicio_fase") @RequestParam(required = false) String eventType,
            @Parameter(description = "Desde (ISO date-time sin Z)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Hasta (ISO date-time sin Z)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Página", example = "0") @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño", example = "500") @RequestParam(required = false, defaultValue = "500") Integer size,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        PageResponse<StationResponses.EventResponse> data = PageResponse.from(
                listUseCase.execute(
                        productionOrderId, userId, phase, processKey, eventType, from, to,
                        PageQuery.of(page, size), authentication),
                StationResponses.EventResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerStationEvent",
            summary = "Registrar evento genérico de estación",
            description = "Registra un evento indicando el tipo en query param `eventType`. " + EVENT_CONTRACT
    )
    @ApiResponse(
            responseCode = "201",
            description = "Evento creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationEventSuccessEnvelope.class),
                    examples = @ExampleObject(name = "EventoCreado", value = StationSwaggerExamples.EVENT_CREATED)
            )
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationRequests.EventRequest.class),
                    examples = @ExampleObject(name = "Avance", value = StationSwaggerExamples.ADVANCE_REQUEST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.EventResponse>> registerGeneric(
            @Valid @org.springframework.web.bind.annotation.RequestBody StationRequests.EventRequest request,
            @Parameter(
                    description = "Tipo de evento",
                    example = "avance_unidades",
                    schema = @Schema(allowableValues = {
                            "avance_unidades", "paro", "reanudacion", "inicio_fase", "fin_fase",
                            "entrega_parcial", "entrega_total", "marca_horario", "asignacion", "cambio_estado_orden"
                    })
            )
            @RequestParam(defaultValue = "avance_unidades") String eventType,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        StationRequests.RegisterStationEventPayload payload = new StationRequests.RegisterStationEventPayload(
                request.productionOrderId(),
                request.workName(),
                request.phase(),
                request.processKey(),
                request.userId(),
                request.units(),
                request.productionStatus(),
                request.note(),
                request.pauseReason() != null ? request.pauseReason() : request.reason(),
                request.occurredAt(),
                Boolean.TRUE.equals(request.isShiftEvent()),
                StationEventType.fromValue(eventType)
        );
        return created(payload, authentication, httpRequest);
    }

    @PostMapping("/advance")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerStationAdvance",
            summary = "Registrar avance de unidades",
            description = "Requiere `units` > 0. " + EVENT_CONTRACT
    )
    @ApiResponse(
            responseCode = "201",
            description = "Avance registrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationEventSuccessEnvelope.class),
                    examples = @ExampleObject(name = "AvanceCreado", value = StationSwaggerExamples.EVENT_CREATED)
            )
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationRequests.EventRequest.class),
                    examples = @ExampleObject(name = "Avance", value = StationSwaggerExamples.ADVANCE_REQUEST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.EventResponse>> advance(
            @Valid @org.springframework.web.bind.annotation.RequestBody StationRequests.EventRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return created(StationRequests.toAdvance(request), authentication, httpRequest);
    }

    @PostMapping("/pause")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerStationPause",
            summary = "Registrar pausa",
            description = "Requiere `pauseReason`. Cierra intervalo laboral abierto y abre pausa. " + EVENT_CONTRACT
    )
    @ApiResponse(
            responseCode = "201",
            description = "Pausa registrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationEventSuccessEnvelope.class),
                    examples = @ExampleObject(name = "PausaCreada", value = StationSwaggerExamples.EVENT_CREATED)
            )
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationRequests.EventRequest.class),
                    examples = @ExampleObject(name = "Pausa", value = StationSwaggerExamples.PAUSE_REQUEST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.EventResponse>> pause(
            @Valid @org.springframework.web.bind.annotation.RequestBody StationRequests.EventRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return created(StationRequests.toPause(request), authentication, httpRequest);
    }

    @PostMapping("/resume")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerStationResume",
            summary = "Reanudar después de pausa",
            description = "Requiere pausa abierta en el mismo processKey/operario. " + EVENT_CONTRACT
    )
    @ApiResponse(
            responseCode = "201",
            description = "Reanudación registrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationEventSuccessEnvelope.class),
                    examples = @ExampleObject(name = "EventoCreado", value = StationSwaggerExamples.EVENT_CREATED)
            )
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationRequests.EventRequest.class),
                    examples = @ExampleObject(name = "Reanudar", value = StationSwaggerExamples.RESUME_REQUEST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.EventResponse>> resume(
            @Valid @org.springframework.web.bind.annotation.RequestBody StationRequests.EventRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return created(StationRequests.toResume(request), authentication, httpRequest);
    }

    @PostMapping("/delivery/partial")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerStationPartialDelivery",
            summary = "Registrar entrega parcial",
            description = "Requiere `units` > 0 y unidades procesadas disponibles. " + EVENT_CONTRACT
    )
    @ApiResponse(
            responseCode = "201",
            description = "Entrega parcial registrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationEventSuccessEnvelope.class),
                    examples = @ExampleObject(name = "EventoCreado", value = StationSwaggerExamples.EVENT_CREATED)
            )
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationRequests.EventRequest.class),
                    examples = @ExampleObject(name = "EntregaParcial", value = StationSwaggerExamples.DELIVERY_PARTIAL_REQUEST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.EventResponse>> partialDelivery(
            @Valid @org.springframework.web.bind.annotation.RequestBody StationRequests.EventRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return created(StationRequests.toPartialDelivery(request), authentication, httpRequest);
    }

    @PostMapping("/delivery/total")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerStationTotalDelivery",
            summary = "Registrar entrega total",
            description = "Requiere `units` > 0. " + EVENT_CONTRACT
    )
    @ApiResponse(
            responseCode = "201",
            description = "Entrega total registrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationEventSuccessEnvelope.class),
                    examples = @ExampleObject(name = "EventoCreado", value = StationSwaggerExamples.EVENT_CREATED)
            )
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationRequests.EventRequest.class),
                    examples = @ExampleObject(name = "EntregaTotal", value = StationSwaggerExamples.DELIVERY_TOTAL_REQUEST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.EventResponse>> totalDelivery(
            @Valid @org.springframework.web.bind.annotation.RequestBody StationRequests.EventRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return created(StationRequests.toTotalDelivery(request), authentication, httpRequest);
    }

    @PostMapping("/shift-mark")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerStationShiftMark",
            summary = "Marcar inicio/fin de jornada",
            description = "Usar `reason`/`pauseReason` = inicio_horario | fin_horario y `isShiftEvent=true`. " + EVENT_CONTRACT
    )
    @ApiResponse(
            responseCode = "201",
            description = "Marca de jornada registrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationEventSuccessEnvelope.class),
                    examples = @ExampleObject(name = "EventoCreado", value = StationSwaggerExamples.EVENT_CREATED)
            )
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationRequests.EventRequest.class),
                    examples = @ExampleObject(name = "Jornada", value = StationSwaggerExamples.SHIFT_MARK_REQUEST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.EventResponse>> shiftMark(
            @Valid @org.springframework.web.bind.annotation.RequestBody StationRequests.EventRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return created(StationRequests.toShiftMark(request), authentication, httpRequest);
    }

    @PostMapping("/phase/start")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerStationPhaseStart",
            summary = "Iniciar fase/proceso",
            description = "Abre intervalo laboral y actualiza progreso del proceso. " + EVENT_CONTRACT
    )
    @ApiResponse(
            responseCode = "201",
            description = "Inicio de fase registrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationEventSuccessEnvelope.class),
                    examples = @ExampleObject(name = "EventoCreado", value = StationSwaggerExamples.EVENT_CREATED)
            )
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationRequests.EventRequest.class),
                    examples = @ExampleObject(name = "InicioFase", value = StationSwaggerExamples.PHASE_START_REQUEST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.EventResponse>> phaseStart(
            @Valid @org.springframework.web.bind.annotation.RequestBody StationRequests.EventRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return created(StationRequests.toPhaseStart(request), authentication, httpRequest);
    }

    @PostMapping("/phase/end")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerStationPhaseEnd",
            summary = "Finalizar fase/proceso",
            description = "Cierra el intervalo laboral abierto del processKey. " + EVENT_CONTRACT
    )
    @ApiResponse(
            responseCode = "201",
            description = "Fin de fase registrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationEventSuccessEnvelope.class),
                    examples = @ExampleObject(name = "EventoCreado", value = StationSwaggerExamples.EVENT_CREATED)
            )
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StationRequests.EventRequest.class),
                    examples = @ExampleObject(name = "FinFase", value = StationSwaggerExamples.PHASE_END_REQUEST)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<StationResponses.EventResponse>> phaseEnd(
            @Valid @org.springframework.web.bind.annotation.RequestBody StationRequests.EventRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return created(StationRequests.toPhaseEnd(request), authentication, httpRequest);
    }

    private ResponseEntity<ApiSuccessEnvelope<StationResponses.EventResponse>> created(
            StationRequests.RegisterStationEventPayload payload,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var saved = registerUseCase.execute(new RegisterStationEventCommand(
                payload.productionOrderId(),
                payload.workName(),
                payload.phase(),
                payload.processKey(),
                payload.userId(),
                payload.units(),
                payload.productionStatus(),
                payload.note(),
                payload.pauseReason(),
                payload.occurredAt(),
                payload.shiftEvent(),
                payload.eventType()
        ), authentication);
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Station event created",
                StationResponses.EventResponse.from(saved)
        );
    }
}
