package com.inkcore.infrastructure.in.rest.papers;

import com.inkcore.application.paper.usecase.CreatePaperCommand;
import com.inkcore.application.paper.usecase.CreatePaperUseCase;
import com.inkcore.application.paper.usecase.GetPaperByIdUseCase;
import com.inkcore.application.paper.usecase.ListPapersUseCase;
import com.inkcore.application.paper.usecase.UpdatePaperCommand;
import com.inkcore.application.paper.usecase.UpdatePaperUseCase;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.PaperListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.PaperSuccessEnvelope;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/papers")
@Tag(name = "Papeles", description = "Catálogo (acceptsRemnants + minRemnant W/H/unit), precios, despieces, stock y remanentes.")
@SecurityRequirement(name = "bearerAuth")
public class PaperController {

    private final CreatePaperUseCase createPaperUseCase;
    private final UpdatePaperUseCase updatePaperUseCase;
    private final GetPaperByIdUseCase getPaperByIdUseCase;
    private final ListPapersUseCase listPapersUseCase;
    private final ApiResponseFactory responseFactory;

    public PaperController(
            CreatePaperUseCase createPaperUseCase,
            UpdatePaperUseCase updatePaperUseCase,
            GetPaperByIdUseCase getPaperByIdUseCase,
            ListPapersUseCase listPapersUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createPaperUseCase = createPaperUseCase;
        this.updatePaperUseCase = updatePaperUseCase;
        this.getPaperByIdUseCase = getPaperByIdUseCase;
        this.listPapersUseCase = listPapersUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerPaper",
            summary = "Crea un papel nuevo.",
            description = "Crea un papel (material + formato width/height/unit + coated) en la compañía del JWT. "
                    + "Política de remanentes (guía UI): acceptsRemnants, minRemnantWidth, minRemnantHeight, "
                    + "minRemnantUnit (si accepts=true; unit default=unit del pliego). "
                    + "Unicidad: name + grammage + width + height + unit. "
                    + "Precios, despieces, stock y remanentes son endpoints anidados bajo /papers/{paperId}/…"
    )
    @ApiResponse(
            responseCode = "201",
            description = "Papel creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "PapelCreado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Paper created"
                                      },
                                      "timestamp": "2026-08-04T12:00:00Z",
                                      "data": {
                                        "paperId": "paper-seed-001",
                                        "companyId": "company-seed-001",
                                        "name": "Bond 75",
                                        "grammage": 75.00,
                                        "width": 70.00,
                                        "height": 100.00,
                                        "unit": "cm",
                                        "coated": false,
                                        "acceptsRemnants": true,
                                        "minRemnantWidth": 20.00,
                                        "minRemnantHeight": 20.00,
                                        "minRemnantUnit": "cm",
                                        "state": true,
                                        "creationDate": "2026-08-04",
                                        "updatedAt": "2026-08-04T12:00:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nuevo papel",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaperRequests.CreatePaperRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoPapel",
                                    value = """
                                            {
                                              "name": "Bond 75",
                                              "grammage": 75.00,
                                              "width": 70.00,
                                              "height": 100.00,
                                              "unit": "cm",
                                              "coated": false,
                                              "acceptsRemnants": true,
                                              "minRemnantWidth": 20.00,
                                              "minRemnantHeight": 20.00,
                                              "minRemnantUnit": "cm",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody PaperRequests.CreatePaperRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var created = createPaperUseCase.execute(
                new CreatePaperCommand(
                        request.name(), request.grammage(), request.width(), request.height(),
                        request.unit(), request.coated(),
                        request.acceptsRemnants(), request.minRemnantWidth(), request.minRemnantHeight(),
                        request.minRemnantUnit(), request.state()),
                authentication
        );
        return responseFactory.created(httpRequest, "CREATED", "Paper created", PaperResponse.from(created));
    }

    @PutMapping("/update/{paperId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updatePaper",
            summary = "Actualiza los datos de un papel existente.",
            description = "Actualiza name, grammage, width, height, unit, coated, "
                    + "acceptsRemnants/minRemnantWidth/minRemnantHeight/minRemnantUnit y state. "
                    + "No cambia companyId. Misma unicidad que el alta (name/gramaje/formato)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Papel actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "PapelActualizado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "OK"
                                      },
                                      "timestamp": "2026-08-04T12:30:00Z",
                                      "data": {
                                        "paperId": "paper-seed-001",
                                        "companyId": "company-seed-001",
                                        "name": "Bond 75",
                                        "grammage": 75.00,
                                        "width": 70.00,
                                        "height": 100.00,
                                        "unit": "cm",
                                        "coated": false,
                                        "acceptsRemnants": true,
                                        "minRemnantWidth": 20.00,
                                        "minRemnantHeight": 20.00,
                                        "minRemnantUnit": "cm",
                                        "state": true,
                                        "creationDate": "2026-08-04",
                                        "updatedAt": "2026-08-04T12:30:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperResponse>> update(
            @Parameter(description = "Identificador del papel", required = true, example = "paper-seed-001")
            @PathVariable String paperId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload de actualización del papel",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaperRequests.UpdatePaperRequest.class),
                            examples = @ExampleObject(
                                    name = "ActualizarPapel",
                                    value = """
                                            {
                                              "name": "Bond 75",
                                              "grammage": 75.00,
                                              "width": 70.00,
                                              "height": 100.00,
                                              "unit": "cm",
                                              "coated": false,
                                              "acceptsRemnants": true,
                                              "minRemnantWidth": 20.00,
                                              "minRemnantHeight": 20.00,
                                              "minRemnantUnit": "cm",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody PaperRequests.UpdatePaperRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var updated = updatePaperUseCase.execute(
                paperId,
                new UpdatePaperCommand(
                        request.name(), request.grammage(), request.width(), request.height(),
                        request.unit(), request.coated(),
                        request.acceptsRemnants(), request.minRemnantWidth(), request.minRemnantHeight(),
                        request.minRemnantUnit(), request.state()),
                authentication
        );
        return responseFactory.success(httpRequest, HttpStatus.OK, PaperResponse.from(updated));
    }

    @GetMapping("/{paperId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getPaper",
            summary = "Consulta el detalle de un papel por su identificador.",
            description = "Consulta el detalle de un papel de la compañía del JWT."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Papel encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "PapelDetalle",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "OK"
                                      },
                                      "timestamp": "2026-08-04T12:00:00Z",
                                      "data": {
                                        "paperId": "paper-seed-001",
                                        "companyId": "company-seed-001",
                                        "name": "Bond 75",
                                        "grammage": 75.00,
                                        "width": 70.00,
                                        "height": 100.00,
                                        "unit": "cm",
                                        "coated": false,
                                        "acceptsRemnants": true,
                                        "minRemnantWidth": 20.00,
                                        "minRemnantHeight": 20.00,
                                        "minRemnantUnit": "cm",
                                        "state": true,
                                        "creationDate": "2026-08-04",
                                        "updatedAt": "2026-08-04T12:00:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PaperResponse>> getById(
            @Parameter(description = "Identificador del papel", required = true, example = "paper-seed-001")
            @PathVariable String paperId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.okStandard(
                httpRequest,
                PaperResponse.from(getPaperByIdUseCase.execute(paperId, authentication))
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listPapers",
            summary = "Obtiene el listado paginado de papeles.",
            description = "Listado paginado de la compañía del JWT. "
                    + "Filtros opcionales: state, coated, acceptsRemnants."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de papeles",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaperListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ListadoPapeles",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "OK"
                                      },
                                      "timestamp": "2026-08-04T12:00:00Z",
                                      "data": {
                                        "content": [
                                          {
                                            "paperId": "paper-seed-001",
                                            "companyId": "company-seed-001",
                                            "name": "Bond 75",
                                            "grammage": 75.00,
                                            "width": 70.00,
                                            "height": 100.00,
                                            "unit": "cm",
                                            "coated": false,
                                            "acceptsRemnants": true,
                                            "minRemnantWidth": 20.00,
                                            "minRemnantHeight": 20.00,
                                            "minRemnantUnit": "cm",
                                            "state": true,
                                            "creationDate": "2026-08-04",
                                            "updatedAt": "2026-08-04T12:00:00"
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
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<PaperResponse>>> list(
            @Parameter(description = "Filtro por estado: true=activos, false=inactivos, omitir=todos")
            @RequestParam(required = false) Boolean state,
            @Parameter(description = "Filtro por esmaltado: true=esmaltados, false=no esmaltados, omitir=todos")
            @RequestParam(required = false) Boolean coated,
            @Parameter(description = "Filtro: true=aceptan remanentes, false=no aceptan, omitir=todos")
            @RequestParam(required = false) Boolean acceptsRemnants,
            @Parameter(description = "Página 0-based", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página (máx 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var result = listPapersUseCase.execute(
                state, coated, acceptsRemnants, PageQuery.of(page, size), authentication);
        return responseFactory.okStandard(
                httpRequest,
                PageResponse.from(result, PaperResponse::from)
        );
    }
}
