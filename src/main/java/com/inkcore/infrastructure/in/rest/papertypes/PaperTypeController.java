package com.inkcore.infrastructure.in.rest.papertypes;

import com.inkcore.application.papertype.usecase.CreatePaperTypeCommand;
import com.inkcore.application.papertype.usecase.CreatePaperTypeUseCase;
import com.inkcore.application.papertype.usecase.GetPaperTypeByIdUseCase;
import com.inkcore.application.papertype.usecase.ListPaperTypesUseCase;
import com.inkcore.application.papertype.usecase.PaperTypeCutAssignmentCommand;
import com.inkcore.application.papertype.usecase.UpdatePaperTypeCommand;
import com.inkcore.application.papertype.usecase.UpdatePaperTypeUseCase;
import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.PaperTypeListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.PaperTypeSuccessEnvelope;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/paper-types")
@Tag(name = "Tipos de papel", description = "Gestión de tipos de papel y despieces asociados")
@SecurityRequirement(name = "bearerAuth")
public class PaperTypeController {

    private final CreatePaperTypeUseCase createPaperTypeUseCase;
    private final UpdatePaperTypeUseCase updatePaperTypeUseCase;
    private final ListPaperTypesUseCase listPaperTypesUseCase;
    private final GetPaperTypeByIdUseCase getPaperTypeByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public PaperTypeController(
            CreatePaperTypeUseCase createPaperTypeUseCase,
            UpdatePaperTypeUseCase updatePaperTypeUseCase,
            ListPaperTypesUseCase listPaperTypesUseCase,
            GetPaperTypeByIdUseCase getPaperTypeByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createPaperTypeUseCase = createPaperTypeUseCase;
        this.updatePaperTypeUseCase = updatePaperTypeUseCase;
        this.listPaperTypesUseCase = listPaperTypesUseCase;
        this.getPaperTypeByIdUseCase = getPaperTypeByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "registerPaperType",
            summary = "Crea un tipo de papel nuevo.",
            description = "Crea un tipo de papel nuevo (formulario Nuevo tipo de papel), con despieces opcionales."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Tipo de papel creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperTypeSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "TipoPapelCreado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Paper type created"
                                      },
                                      "timestamp": "2026-08-01T12:00:00Z",
                                      "data": {
                                        "paperTypeId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Bond 75g",
                                        "width": 70.00,
                                        "height": 100.00,
                                        "unit": "cm",
                                        "sheetValue": 1500.00,
                                        "packageUnit": 500,
                                        "isCoated": false,
                                        "state": true,
                                        "creationDate": "2026-08-01",
                                        "cutLayouts": [
                                          {
                                            "cutLayoutId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                            "cutValue": 200.00
                                          }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperTypeResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nuevo tipo de papel",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreatePaperTypeRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoTipoPapel",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "name": "Bond 75g",
                                              "width": 70.00,
                                              "height": 100.00,
                                              "unit": "cm",
                                              "sheetValue": 1500.00,
                                              "packageUnit": 500,
                                              "isCoated": false,
                                              "state": true,
                                              "cutLayouts": [
                                                {
                                                  "cutLayoutId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                                  "cutValue": 200.00
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreatePaperTypeRequest request,
            HttpServletRequest httpRequest
    ) {
        PaperType created = createPaperTypeUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Paper type created",
                PaperTypeResponse.from(created)
        );
    }

    @PutMapping("/update/{paperTypeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "updatePaperType",
            summary = "Actualiza los datos de un tipo de papel existente.",
            description = "Actualiza un tipo de papel y reemplaza sus despieces asociados."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tipo de papel actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperTypeSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperTypeResponse>> update(
            @Parameter(description = "Identificador del tipo de papel", required = true,
                    example = "814ad646-c4fe-42fa-9f13-4a44823e6bee")
            @PathVariable String paperTypeId,
            @Valid @RequestBody UpdatePaperTypeRequest request,
            HttpServletRequest httpRequest
    ) {
        PaperType updated = updatePaperTypeUseCase.execute(toUpdateCommand(paperTypeId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, PaperTypeResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "listPaperTypes",
            summary = "Obtiene el listado paginado de tipos de papel.",
            description = "Obtiene el listado paginado de tipos de papel."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de tipos de papel",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperTypeListSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<PaperTypeResponse>>> list(
            @Parameter(description = "Filtro por empresa", example = "company-seed-001")
            @RequestParam(required = false) String companyId,
            @Parameter(description = "Filtro por estado: true=activos, false=inactivos, omitir=todos")
            @RequestParam(required = false) Boolean state,
            @Parameter(description = "Página 0-based", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página (máx 100)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,
            HttpServletRequest httpRequest
    ) {
        PageResponse<PaperTypeResponse> data = PageResponse.from(
                listPaperTypesUseCase.execute(companyId, state, PageQuery.of(page, size)),
                PaperTypeResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{paperTypeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "getPaperType",
            summary = "Consulta el detalle de un tipo de papel por su identificador.",
            description = "Consulta el detalle de un tipo de papel por su identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tipo de papel encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperTypeSuccessEnvelope.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Tipo de papel no encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperTypeResponse>> get(
            @Parameter(description = "Identificador del tipo de papel", required = true,
                    example = "814ad646-c4fe-42fa-9f13-4a44823e6bee")
            @PathVariable String paperTypeId,
            HttpServletRequest httpRequest
    ) {
        PaperType paperType = getPaperTypeByIdUseCase.execute(paperTypeId);
        return responseFactory.success(httpRequest, HttpStatus.OK, PaperTypeResponse.from(paperType));
    }

    private static CreatePaperTypeCommand toCreateCommand(CreatePaperTypeRequest request) {
        return new CreatePaperTypeCommand(
                request.companyId(),
                request.name(),
                request.width(),
                request.height(),
                request.unit(),
                request.sheetValue(),
                request.packageUnit(),
                request.isCoated(),
                request.state(),
                toAssignmentCommands(request.cutLayouts())
        );
    }

    private static UpdatePaperTypeCommand toUpdateCommand(String paperTypeId, UpdatePaperTypeRequest request) {
        return new UpdatePaperTypeCommand(
                paperTypeId,
                request.name(),
                request.width(),
                request.height(),
                request.unit(),
                request.sheetValue(),
                request.packageUnit(),
                Boolean.TRUE.equals(request.isCoated()),
                Boolean.TRUE.equals(request.state()),
                toAssignmentCommands(request.cutLayouts())
        );
    }

    private static List<PaperTypeCutAssignmentCommand> toAssignmentCommands(List<PaperTypeCutLayoutRequest> cutLayouts) {
        if (cutLayouts == null) {
            return List.of();
        }
        return cutLayouts.stream()
                .map(c -> new PaperTypeCutAssignmentCommand(c.cutLayoutId(), c.cutValue()))
                .toList();
    }
}
