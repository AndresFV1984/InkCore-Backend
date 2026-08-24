package com.inkcore.infrastructure.in.rest.assemblyprices;

import com.inkcore.application.assemblyprice.usecase.CreateAssemblyPriceCommand;
import com.inkcore.application.assemblyprice.usecase.CreateAssemblyPriceUseCase;
import com.inkcore.application.assemblyprice.usecase.GetAssemblyPriceByIdUseCase;
import com.inkcore.application.assemblyprice.usecase.ListAssemblyPricesUseCase;
import com.inkcore.application.assemblyprice.usecase.UpdateAssemblyPriceCommand;
import com.inkcore.application.assemblyprice.usecase.UpdateAssemblyPriceUseCase;
import com.inkcore.domain.assemblyprice.model.AssemblyPrice;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.AssemblyPriceListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.AssemblyPriceSuccessEnvelope;
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
@RequestMapping("/api/v1/assembly-prices")
@Tag(name = "Precios de montaje", description = "Catálogo de precios de montaje")
@SecurityRequirement(name = "bearerAuth")
public class AssemblyPriceController {

    private final CreateAssemblyPriceUseCase createAssemblyPriceUseCase;
    private final UpdateAssemblyPriceUseCase updateAssemblyPriceUseCase;
    private final ListAssemblyPricesUseCase listAssemblyPricesUseCase;
    private final GetAssemblyPriceByIdUseCase getAssemblyPriceByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public AssemblyPriceController(
            CreateAssemblyPriceUseCase createAssemblyPriceUseCase,
            UpdateAssemblyPriceUseCase updateAssemblyPriceUseCase,
            ListAssemblyPricesUseCase listAssemblyPricesUseCase,
            GetAssemblyPriceByIdUseCase getAssemblyPriceByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createAssemblyPriceUseCase = createAssemblyPriceUseCase;
        this.updateAssemblyPriceUseCase = updateAssemblyPriceUseCase;
        this.listAssemblyPricesUseCase = listAssemblyPricesUseCase;
        this.getAssemblyPriceByIdUseCase = getAssemblyPriceByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerAssemblyPrice",
            summary = "Crea un precio de montaje nuevo.",
            description = "Crea un precio de montaje (formulario Nuevo precio de montaje). Nombre único por empresa. Sin delete físico: inactivar con state=false."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Precio de montaje creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AssemblyPriceSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "PrecioMontajeCreado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Assembly price created"
                                      },
                                      "timestamp": "2026-08-08T12:00:00Z",
                                      "data": {
                                        "assemblyPriceId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Montaje estándar 4 tintas",
                                        "cost": 85000.00,
                                        "state": true,
                                        "creationDate": "2026-08-08"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<AssemblyPriceResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nuevo precio de montaje",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateAssemblyPriceRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoPrecioMontaje",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "name": "Montaje estándar 4 tintas",
                                              "cost": 85000.00,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateAssemblyPriceRequest request,
            HttpServletRequest httpRequest
    ) {
        AssemblyPrice created = createAssemblyPriceUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Assembly price created",
                AssemblyPriceResponse.from(created)
        );
    }

    @PutMapping("/update/{assemblyPriceId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateAssemblyPrice",
            summary = "Actualiza los datos de un precio de montaje existente.",
            description = "Actualiza nombre, costo y estado. No cambia companyId. Nombre único por empresa."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Precio de montaje actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AssemblyPriceSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "PrecioMontajeActualizado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "OK"
                                      },
                                      "timestamp": "2026-08-08T12:30:00Z",
                                      "data": {
                                        "assemblyPriceId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Montaje estándar 4 tintas",
                                        "cost": 90000.00,
                                        "state": true,
                                        "creationDate": "2026-08-08"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<AssemblyPriceResponse>> update(
            @Parameter(description = "Identificador del precio de montaje", required = true,
                    example = "814ad646-c4fe-42fa-9f13-4a44823e6bee")
            @PathVariable String assemblyPriceId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload de actualización del precio de montaje",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateAssemblyPriceRequest.class),
                            examples = @ExampleObject(
                                    name = "ActualizarPrecioMontaje",
                                    value = """
                                            {
                                              "name": "Montaje estándar 4 tintas",
                                              "cost": 90000.00,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateAssemblyPriceRequest request,
            HttpServletRequest httpRequest
    ) {
        AssemblyPrice updated = updateAssemblyPriceUseCase.execute(toUpdateCommand(assemblyPriceId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, AssemblyPriceResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listAssemblyPrices",
            summary = "Obtiene el listado paginado de precios de montaje.",
            description = "Listado paginado. Filtros opcionales: companyId y state."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de precios de montaje",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AssemblyPriceListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ListadoPreciosMontaje",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "OK"
                                      },
                                      "timestamp": "2026-08-08T12:00:00Z",
                                      "data": {
                                        "content": [
                                          {
                                            "assemblyPriceId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                            "companyId": "company-seed-001",
                                            "name": "Montaje estándar 4 tintas",
                                            "cost": 85000.00,
                                            "state": true,
                                            "creationDate": "2026-08-08"
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
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<AssemblyPriceResponse>>> list(
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
        PageResponse<AssemblyPriceResponse> data = PageResponse.from(
                listAssemblyPricesUseCase.execute(companyId, state, PageQuery.of(page, size)),
                AssemblyPriceResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{assemblyPriceId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getAssemblyPrice",
            summary = "Consulta el detalle de un precio de montaje por su identificador.",
            description = "Consulta el detalle de un precio de montaje por su identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Precio de montaje encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AssemblyPriceSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "PrecioMontajeDetalle",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "OK"
                                      },
                                      "timestamp": "2026-08-08T12:00:00Z",
                                      "data": {
                                        "assemblyPriceId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Montaje estándar 4 tintas",
                                        "cost": 85000.00,
                                        "state": true,
                                        "creationDate": "2026-08-08"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Precio de montaje no encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<AssemblyPriceResponse>> get(
            @Parameter(description = "Identificador del precio de montaje", required = true,
                    example = "814ad646-c4fe-42fa-9f13-4a44823e6bee")
            @PathVariable String assemblyPriceId,
            HttpServletRequest httpRequest
    ) {
        AssemblyPrice assemblyPrice = getAssemblyPriceByIdUseCase.execute(assemblyPriceId);
        return responseFactory.success(httpRequest, HttpStatus.OK, AssemblyPriceResponse.from(assemblyPrice));
    }

    private static CreateAssemblyPriceCommand toCreateCommand(CreateAssemblyPriceRequest request) {
        return new CreateAssemblyPriceCommand(
                request.companyId(),
                request.name(),
                request.cost(),
                request.state()
        );
    }

    private static UpdateAssemblyPriceCommand toUpdateCommand(
            String assemblyPriceId,
            UpdateAssemblyPriceRequest request
    ) {
        return new UpdateAssemblyPriceCommand(
                assemblyPriceId,
                request.name(),
                request.cost(),
                Boolean.TRUE.equals(request.state())
        );
    }
}
