package com.inkcore.infrastructure.in.rest.finishingprocesses;

import com.inkcore.application.finishingprocess.usecase.CreateFinishingProcessCommand;
import com.inkcore.application.finishingprocess.usecase.CreateFinishingProcessUseCase;
import com.inkcore.application.finishingprocess.usecase.GetFinishingProcessByIdUseCase;
import com.inkcore.application.finishingprocess.usecase.ListFinishingProcessesUseCase;
import com.inkcore.application.finishingprocess.usecase.UpdateFinishingProcessCommand;
import com.inkcore.application.finishingprocess.usecase.UpdateFinishingProcessUseCase;
import com.inkcore.domain.finishingprocess.model.FinishingProcess;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.FinishingProcessListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.FinishingProcessSuccessEnvelope;
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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finishing-processes")
@Tag(name = "Acabados", description = "Gestión de acabados")
@SecurityRequirement(name = "bearerAuth")
public class FinishingProcessController {

    private final CreateFinishingProcessUseCase createFinishingProcessUseCase;
    private final UpdateFinishingProcessUseCase updateFinishingProcessUseCase;
    private final ListFinishingProcessesUseCase listFinishingProcessesUseCase;
    private final GetFinishingProcessByIdUseCase getFinishingProcessByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public FinishingProcessController(
            CreateFinishingProcessUseCase createFinishingProcessUseCase,
            UpdateFinishingProcessUseCase updateFinishingProcessUseCase,
            ListFinishingProcessesUseCase listFinishingProcessesUseCase,
            GetFinishingProcessByIdUseCase getFinishingProcessByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createFinishingProcessUseCase = createFinishingProcessUseCase;
        this.updateFinishingProcessUseCase = updateFinishingProcessUseCase;
        this.listFinishingProcessesUseCase = listFinishingProcessesUseCase;
        this.getFinishingProcessByIdUseCase = getFinishingProcessByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "registerFinishingProcess",
            summary = "Registrar proceso de acabado",
            description = """
                    Crea un proceso de acabado (formulario Nueva operación de acabado).
                    `POST /api/v1/finishing-processes/register`
                    Obligatorios: companyId, name.
                    Opcionales: minCost, valuePerCm2 (default 0), quickAccess (default false), state (default true).
                    Si name ya existe en la misma empresa → 409 CONFLICT.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Proceso de acabado creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FinishingProcessSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "AcabadoCreado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Finishing process created"
                                      },
                                      "timestamp": "2026-07-28T12:00:00Z",
                                      "data": {
                                        "finishingProcessId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Plegar",
                                        "minCost": 5000.00,
                                        "valuePerCm2": 1200.00,
                                        "quickAccess": true,
                                        "state": true,
                                        "creationDate": "2026-07-28"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<FinishingProcessResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nueva operación de acabado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateFinishingProcessRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevaOperacionAcabado",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "name": "Plegar",
                                              "minCost": 5000.00,
                                              "valuePerCm2": 1200.00,
                                              "quickAccess": true,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateFinishingProcessRequest request,
            HttpServletRequest httpRequest
    ) {
        FinishingProcess created = createFinishingProcessUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Finishing process created",
                FinishingProcessResponse.from(created)
        );
    }

    @PutMapping("/update/{finishingProcessId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "updateFinishingProcess",
            summary = "Actualizar proceso de acabado",
            description = """
                    `PUT /api/v1/finishing-processes/update/{finishingProcessId}`
                    El body no incluye `companyId`.
                    Obligatorios: name, quickAccess, state.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Proceso de acabado actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FinishingProcessSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<FinishingProcessResponse>> update(
            @Parameter(description = "Identificador del proceso de acabado", required = true, example = "714ad646-c4fe-42fa-9f13-4a44823e6bee")
            @PathVariable String finishingProcessId,
            @Valid @RequestBody UpdateFinishingProcessRequest request,
            HttpServletRequest httpRequest
    ) {
        FinishingProcess updated = updateFinishingProcessUseCase.execute(toUpdateCommand(finishingProcessId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, FinishingProcessResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "listFinishingProcesses",
            summary = "Listar procesos de acabado",
            description = """
                    `GET /api/v1/finishing-processes/list`
                    Query: `companyId`, `state`, `page`, `size`.
                    Orden: acceso rápido primero, luego nombre.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de procesos de acabado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FinishingProcessListSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<FinishingProcessResponse>>> list(
            @Parameter(description = "Filtro por empresa", example = "company-seed-001")
            @RequestParam(required = false) String companyId,
            @Parameter(description = "Filtro por estado")
            @RequestParam(required = false) Boolean state,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            HttpServletRequest httpRequest
    ) {
        PageResponse<FinishingProcessResponse> data = PageResponse.from(
                listFinishingProcessesUseCase.execute(companyId, state, PageQuery.of(page, size)),
                FinishingProcessResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{finishingProcessId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "getFinishingProcess",
            summary = "Consultar proceso de acabado por ID",
            description = "`GET /api/v1/finishing-processes/get/{finishingProcessId}`"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Proceso de acabado encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FinishingProcessSuccessEnvelope.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Proceso de acabado no encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<FinishingProcessResponse>> get(
            @Parameter(description = "Identificador del proceso de acabado", required = true)
            @PathVariable String finishingProcessId,
            HttpServletRequest httpRequest
    ) {
        FinishingProcess process = getFinishingProcessByIdUseCase.execute(finishingProcessId);
        return responseFactory.success(httpRequest, HttpStatus.OK, FinishingProcessResponse.from(process));
    }

    private static CreateFinishingProcessCommand toCreateCommand(CreateFinishingProcessRequest request) {
        return new CreateFinishingProcessCommand(
                request.companyId(),
                request.name(),
                request.minCost(),
                request.valuePerCm2(),
                request.quickAccess(),
                request.state()
        );
    }

    private static UpdateFinishingProcessCommand toUpdateCommand(
            String finishingProcessId,
            UpdateFinishingProcessRequest request
    ) {
        return new UpdateFinishingProcessCommand(
                finishingProcessId,
                request.name(),
                request.minCost(),
                request.valuePerCm2(),
                Boolean.TRUE.equals(request.quickAccess()),
                Boolean.TRUE.equals(request.state())
        );
    }
}
