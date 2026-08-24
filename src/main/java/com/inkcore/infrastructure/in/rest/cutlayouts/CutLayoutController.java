package com.inkcore.infrastructure.in.rest.cutlayouts;

import com.inkcore.application.cutlayout.usecase.CreateCutLayoutCommand;
import com.inkcore.application.cutlayout.usecase.CreateCutLayoutUseCase;
import com.inkcore.application.cutlayout.usecase.GetCutLayoutByIdUseCase;
import com.inkcore.application.cutlayout.usecase.ListCutLayoutsUseCase;
import com.inkcore.application.cutlayout.usecase.UpdateCutLayoutCommand;
import com.inkcore.application.cutlayout.usecase.UpdateCutLayoutUseCase;
import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.CutLayoutListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.CutLayoutSuccessEnvelope;
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
@RequestMapping("/api/v1/cut-layouts")
@Tag(name = "Despieces", description = "Catálogo de despieces / patrones de corte")
@SecurityRequirement(name = "bearerAuth")
public class CutLayoutController {

    private final CreateCutLayoutUseCase createCutLayoutUseCase;
    private final UpdateCutLayoutUseCase updateCutLayoutUseCase;
    private final ListCutLayoutsUseCase listCutLayoutsUseCase;
    private final GetCutLayoutByIdUseCase getCutLayoutByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public CutLayoutController(
            CreateCutLayoutUseCase createCutLayoutUseCase,
            UpdateCutLayoutUseCase updateCutLayoutUseCase,
            ListCutLayoutsUseCase listCutLayoutsUseCase,
            GetCutLayoutByIdUseCase getCutLayoutByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createCutLayoutUseCase = createCutLayoutUseCase;
        this.updateCutLayoutUseCase = updateCutLayoutUseCase;
        this.listCutLayoutsUseCase = listCutLayoutsUseCase;
        this.getCutLayoutByIdUseCase = getCutLayoutByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerCutLayout",
            summary = "Crea un despiece nuevo.",
            description = "Crea un despiece nuevo (formulario Nuevo despiece)."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Despiece creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CutLayoutSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "DespieceCreado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Cut layout created"
                                      },
                                      "timestamp": "2026-08-01T12:00:00Z",
                                      "data": {
                                        "cutLayoutId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Etiqueta",
                                        "width": 10.00,
                                        "height": 5.00,
                                        "unit": "cm",
                                        "piecesPerSheet": 24,
                                        "state": true,
                                        "creationDate": "2026-08-01"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<CutLayoutResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nuevo despiece",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateCutLayoutRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoDespiece",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "name": "Etiqueta",
                                              "width": 10.00,
                                              "height": 5.00,
                                              "unit": "cm",
                                              "piecesPerSheet": 24,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateCutLayoutRequest request,
            HttpServletRequest httpRequest
    ) {
        CutLayout created = createCutLayoutUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Cut layout created",
                CutLayoutResponse.from(created)
        );
    }

    @PutMapping("/update/{cutLayoutId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateCutLayout",
            summary = "Actualiza los datos de un despiece existente.",
            description = "Actualiza los datos de un despiece existente."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Despiece actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CutLayoutSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "DespieceActualizado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-08-01T12:05:00Z",
                                      "data": {
                                        "cutLayoutId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Etiqueta premium",
                                        "width": 12.00,
                                        "height": 6.00,
                                        "unit": "cm",
                                        "piecesPerSheet": 20,
                                        "state": true,
                                        "creationDate": "2026-08-01"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<CutLayoutResponse>> update(
            @Parameter(
                    description = "Identificador del despiece",
                    required = true,
                    example = "714ad646-c4fe-42fa-9f13-4a44823e6bee"
            )
            @PathVariable String cutLayoutId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos a actualizar (sin companyId)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateCutLayoutRequest.class),
                            examples = @ExampleObject(
                                    name = "ActualizarDespiece",
                                    value = """
                                            {
                                              "name": "Etiqueta premium",
                                              "width": 12.00,
                                              "height": 6.00,
                                              "unit": "cm",
                                              "piecesPerSheet": 20,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateCutLayoutRequest request,
            HttpServletRequest httpRequest
    ) {
        CutLayout updated = updateCutLayoutUseCase.execute(toUpdateCommand(cutLayoutId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, CutLayoutResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listCutLayouts",
            summary = "Obtiene el listado paginado de despieces.",
            description = "Obtiene el listado paginado de despieces."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de despieces",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CutLayoutListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "DespiecesPaginados",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-08-01T12:00:00Z",
                                      "data": {
                                        "content": [
                                          {
                                            "cutLayoutId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                            "companyId": "company-seed-001",
                                            "name": "Etiqueta",
                                            "width": 10.00,
                                            "height": 5.00,
                                            "unit": "cm",
                                            "piecesPerSheet": 24,
                                            "state": true,
                                            "creationDate": "2026-08-01"
                                          }
                                        ],
                                        "page": 0,
                                        "size": 20,
                                        "totalElements": 1,
                                        "totalPages": 1,
                                        "hasNext": false
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<CutLayoutResponse>>> list(
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
        PageResponse<CutLayoutResponse> data = PageResponse.from(
                listCutLayoutsUseCase.execute(companyId, state, PageQuery.of(page, size)),
                CutLayoutResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{cutLayoutId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getCutLayout",
            summary = "Consulta el detalle de un despiece por su identificador.",
            description = "Consulta el detalle de un despiece por su identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Despiece encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CutLayoutSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "DespieceDetalle",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-08-01T12:00:00Z",
                                      "data": {
                                        "cutLayoutId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Etiqueta",
                                        "width": 10.00,
                                        "height": 5.00,
                                        "unit": "cm",
                                        "piecesPerSheet": 24,
                                        "state": true,
                                        "creationDate": "2026-08-01"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Despiece no encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<CutLayoutResponse>> get(
            @Parameter(
                    description = "Identificador del despiece",
                    required = true,
                    example = "714ad646-c4fe-42fa-9f13-4a44823e6bee"
            )
            @PathVariable String cutLayoutId,
            HttpServletRequest httpRequest
    ) {
        CutLayout cutLayout = getCutLayoutByIdUseCase.execute(cutLayoutId);
        return responseFactory.success(httpRequest, HttpStatus.OK, CutLayoutResponse.from(cutLayout));
    }

    private static CreateCutLayoutCommand toCreateCommand(CreateCutLayoutRequest request) {
        return new CreateCutLayoutCommand(
                request.companyId(),
                request.name(),
                request.width(),
                request.height(),
                request.unit(),
                request.piecesPerSheet(),
                request.state()
        );
    }

    private static UpdateCutLayoutCommand toUpdateCommand(String cutLayoutId, UpdateCutLayoutRequest request) {
        return new UpdateCutLayoutCommand(
                cutLayoutId,
                request.name(),
                request.width(),
                request.height(),
                request.unit(),
                request.piecesPerSheet(),
                Boolean.TRUE.equals(request.state())
        );
    }
}
