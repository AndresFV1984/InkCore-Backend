package com.inkcore.infrastructure.in.rest.finishes;

import com.inkcore.application.finish.usecase.CreateFinishCommand;
import com.inkcore.application.finish.usecase.CreateFinishUseCase;
import com.inkcore.application.finish.usecase.GetFinishByIdUseCase;
import com.inkcore.application.finish.usecase.ListFinishesUseCase;
import com.inkcore.application.finish.usecase.UpdateFinishCommand;
import com.inkcore.application.finish.usecase.UpdateFinishUseCase;
import com.inkcore.domain.finish.model.Finish;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.FinishListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.FinishSuccessEnvelope;
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
@RequestMapping("/api/v1/finished-products")
@Tag(name = "Terminados", description = "Gestión de terminados")
@SecurityRequirement(name = "bearerAuth")
public class FinishController {

    private final CreateFinishUseCase createFinishUseCase;
    private final UpdateFinishUseCase updateFinishUseCase;
    private final ListFinishesUseCase listFinishesUseCase;
    private final GetFinishByIdUseCase getFinishByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public FinishController(
            CreateFinishUseCase createFinishUseCase,
            UpdateFinishUseCase updateFinishUseCase,
            ListFinishesUseCase listFinishesUseCase,
            GetFinishByIdUseCase getFinishByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createFinishUseCase = createFinishUseCase;
        this.updateFinishUseCase = updateFinishUseCase;
        this.listFinishesUseCase = listFinishesUseCase;
        this.getFinishByIdUseCase = getFinishByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerFinishedProduct",
            summary = "Crea un producto terminado nuevo.",
            description = "Crea un producto terminado nuevo."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Producto terminado creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FinishSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ProductoTerminadoCreado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Finished product created"
                                      },
                                      "timestamp": "2026-07-28T12:00:00Z",
                                      "data": {
                                        "finishedProductId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Laminado mate",
                                        "minCost": 15000.00,
                                        "valuePerCm2": 5000.00,
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
    public ResponseEntity<ApiSuccessEnvelope<FinishResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nuevo terminado",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateFinishRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoProductoTerminado",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "name": "Laminado mate",
                                              "minCost": 15000.00,
                                              "valuePerCm2": 5000.00,
                                              "quickAccess": true,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateFinishRequest request,
            HttpServletRequest httpRequest
    ) {
        Finish created = createFinishUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Finished product created",
                FinishResponse.from(created)
        );
    }

    @PutMapping("/update/{finishedProductId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateFinishedProduct",
            summary = "Actualiza los datos de un producto terminado existente.",
            description = "Actualiza los datos de un producto terminado existente."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Producto terminado actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FinishSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ProductoTerminadoActualizado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-28T12:05:00Z",
                                      "data": {
                                        "finishedProductId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Laminado brillante",
                                        "minCost": 18000.00,
                                        "valuePerCm2": 5500.00,
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
    public ResponseEntity<ApiSuccessEnvelope<FinishResponse>> update(
            @Parameter(
                    description = "Identificador del producto terminado",
                    required = true,
                    example = "714ad646-c4fe-42fa-9f13-4a44823e6bee"
            )
            @PathVariable String finishedProductId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos a actualizar (sin companyId)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateFinishRequest.class),
                            examples = @ExampleObject(
                                    name = "ActualizarProductoTerminado",
                                    value = """
                                            {
                                              "name": "Laminado brillante",
                                              "minCost": 18000.00,
                                              "valuePerCm2": 5500.00,
                                              "quickAccess": true,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateFinishRequest request,
            HttpServletRequest httpRequest
    ) {
        Finish updated = updateFinishUseCase.execute(toUpdateCommand(finishedProductId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, FinishResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listFinishedProducts",
            summary = "Obtiene el listado paginado de productos terminados.",
            description = "Obtiene el listado paginado de productos terminados."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de productos terminados",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FinishListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ProductosTerminadosPaginados",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-28T12:00:00Z",
                                      "data": {
                                        "content": [
                                          {
                                            "finishedProductId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                            "companyId": "company-seed-001",
                                            "name": "Laminado mate",
                                            "minCost": 15000.00,
                                            "valuePerCm2": 5000.00,
                                            "quickAccess": true,
                                            "state": true,
                                            "creationDate": "2026-07-28"
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
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<FinishResponse>>> list(
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
        PageResponse<FinishResponse> data = PageResponse.from(
                listFinishesUseCase.execute(companyId, state, PageQuery.of(page, size)),
                FinishResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{finishedProductId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getFinishedProduct",
            summary = "Consulta el detalle de un producto terminado por su identificador.",
            description = "Consulta el detalle de un producto terminado por su identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Producto terminado encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FinishSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ProductoTerminadoDetalle",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-28T12:00:00Z",
                                      "data": {
                                        "finishedProductId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Laminado mate",
                                        "minCost": 15000.00,
                                        "valuePerCm2": 5000.00,
                                        "quickAccess": true,
                                        "state": true,
                                        "creationDate": "2026-07-28"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Producto terminado no encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<FinishResponse>> get(
            @Parameter(
                    description = "Identificador del producto terminado",
                    required = true,
                    example = "714ad646-c4fe-42fa-9f13-4a44823e6bee"
            )
            @PathVariable String finishedProductId,
            HttpServletRequest httpRequest
    ) {
        Finish finish = getFinishByIdUseCase.execute(finishedProductId);
        return responseFactory.success(httpRequest, HttpStatus.OK, FinishResponse.from(finish));
    }

    private static CreateFinishCommand toCreateCommand(CreateFinishRequest request) {
        return new CreateFinishCommand(
                request.companyId(),
                request.name(),
                request.minCost(),
                request.valuePerCm2(),
                request.quickAccess(),
                request.state()
        );
    }

    private static UpdateFinishCommand toUpdateCommand(String finishedProductId, UpdateFinishRequest request) {
        return new UpdateFinishCommand(
                finishedProductId,
                request.name(),
                request.minCost(),
                request.valuePerCm2(),
                Boolean.TRUE.equals(request.quickAccess()),
                Boolean.TRUE.equals(request.state())
        );
    }
}
