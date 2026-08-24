package com.inkcore.infrastructure.in.rest.thousandrates;

import com.inkcore.application.thousandrate.usecase.CreateThousandRateCommand;
import com.inkcore.application.thousandrate.usecase.CreateThousandRateUseCase;
import com.inkcore.application.thousandrate.usecase.GetThousandRateByIdUseCase;
import com.inkcore.application.thousandrate.usecase.ListThousandRatesUseCase;
import com.inkcore.application.thousandrate.usecase.UpdateThousandRateCommand;
import com.inkcore.application.thousandrate.usecase.UpdateThousandRateUseCase;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.thousandrate.model.ThousandRate;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ThousandRateListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ThousandRateSuccessEnvelope;
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
@RequestMapping("/api/v1/thousand-rates")
@Tag(name = "Tarifas por millar", description = "Catálogo de tarifas por millar")
@SecurityRequirement(name = "bearerAuth")
public class ThousandRateController {

    private final CreateThousandRateUseCase createThousandRateUseCase;
    private final UpdateThousandRateUseCase updateThousandRateUseCase;
    private final ListThousandRatesUseCase listThousandRatesUseCase;
    private final GetThousandRateByIdUseCase getThousandRateByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public ThousandRateController(
            CreateThousandRateUseCase createThousandRateUseCase,
            UpdateThousandRateUseCase updateThousandRateUseCase,
            ListThousandRatesUseCase listThousandRatesUseCase,
            GetThousandRateByIdUseCase getThousandRateByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createThousandRateUseCase = createThousandRateUseCase;
        this.updateThousandRateUseCase = updateThousandRateUseCase;
        this.listThousandRatesUseCase = listThousandRatesUseCase;
        this.getThousandRateByIdUseCase = getThousandRateByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerThousandRate",
            summary = "Crea una tarifa por millar nueva.",
            description = "Crea una tarifa por millar (formulario Nueva tarifa por millar). "
                    + "Requiere colorCategory; isDefault opcional (default false). "
                    + "Nombre único por empresa. Sin delete físico: inactivar con state=false."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Tarifa por millar creada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ThousandRateSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "TarifaMillarCreada",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Thousand rate created"
                                      },
                                      "timestamp": "2026-08-08T12:00:00Z",
                                      "data": {
                                        "thousandRateId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Color básico",
                                        "colorCategory": "1 COLOR",
                                        "thousandUnit": 1000,
                                        "price": 17500.00,
                                        "state": true,
                                        "minThresholdUnits": 600,
                                        "minThousand": 500.00,
                                        "decimalThreshold": 0.20,
                                        "gripperFlipPrice": 20000.00,
                                        "squareFlipPrice": 20000.00,
                                        "isDefault": false,
                                        "creationDate": "2026-08-08"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ThousandRateResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nueva tarifa por millar",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateThousandRateRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevaTarifaMillar",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "name": "Color básico",
                                              "colorCategory": "1 COLOR",
                                              "thousandUnit": 1000,
                                              "price": 17500.00,
                                              "state": true,
                                              "minThresholdUnits": 600,
                                              "minThousand": 500.00,
                                              "decimalThreshold": 0.20,
                                              "gripperFlipPrice": 20000.00,
                                              "squareFlipPrice": 20000.00,
                                              "isDefault": false
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateThousandRateRequest request,
            HttpServletRequest httpRequest
    ) {
        ThousandRate created = createThousandRateUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Thousand rate created",
                ThousandRateResponse.from(created)
        );
    }

    @PutMapping("/update/{thousandRateId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateThousandRate",
            summary = "Actualiza los datos de una tarifa por millar existente.",
            description = "Actualiza nombre, colorCategory, unidad, precio, reglas de millar/volteo, isDefault y estado. "
                    + "No cambia companyId. Nombre único por empresa."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tarifa por millar actualizada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ThousandRateSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "TarifaMillarActualizada",
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
                                        "thousandRateId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Color básico",
                                        "colorCategory": "1 COLOR",
                                        "thousandUnit": 1000,
                                        "price": 18000.00,
                                        "state": true,
                                        "minThresholdUnits": 600,
                                        "minThousand": 500.00,
                                        "decimalThreshold": 0.20,
                                        "gripperFlipPrice": 20000.00,
                                        "squareFlipPrice": null,
                                        "isDefault": true,
                                        "creationDate": "2026-08-08"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ThousandRateResponse>> update(
            @Parameter(description = "Identificador de la tarifa por millar", required = true,
                    example = "814ad646-c4fe-42fa-9f13-4a44823e6bee")
            @PathVariable String thousandRateId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload de actualización de la tarifa por millar",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateThousandRateRequest.class),
                            examples = @ExampleObject(
                                    name = "ActualizarTarifaMillar",
                                    value = """
                                            {
                                              "name": "Color básico",
                                              "colorCategory": "1 COLOR",
                                              "thousandUnit": 1000,
                                              "price": 18000.00,
                                              "state": true,
                                              "minThresholdUnits": 600,
                                              "minThousand": 500.00,
                                              "decimalThreshold": 0.20,
                                              "gripperFlipPrice": 20000.00,
                                              "squareFlipPrice": null,
                                              "isDefault": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateThousandRateRequest request,
            HttpServletRequest httpRequest
    ) {
        ThousandRate updated = updateThousandRateUseCase.execute(toUpdateCommand(thousandRateId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, ThousandRateResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listThousandRates",
            summary = "Obtiene el listado paginado de tarifas por millar.",
            description = "Listado paginado. Filtros opcionales: companyId y state."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de tarifas por millar",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ThousandRateListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ListadoTarifasMillar",
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
                                            "thousandRateId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                            "companyId": "company-seed-001",
                                            "name": "Color básico",
                                            "colorCategory": "1 COLOR",
                                            "thousandUnit": 1000,
                                            "price": 17500.00,
                                            "state": true,
                                            "minThresholdUnits": 600,
                                            "minThousand": 500.00,
                                            "decimalThreshold": 0.20,
                                            "gripperFlipPrice": 20000.00,
                                            "squareFlipPrice": 20000.00,
                                            "isDefault": false,
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
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<ThousandRateResponse>>> list(
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
        PageResponse<ThousandRateResponse> data = PageResponse.from(
                listThousandRatesUseCase.execute(companyId, state, PageQuery.of(page, size)),
                ThousandRateResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{thousandRateId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getThousandRate",
            summary = "Consulta el detalle de una tarifa por millar por su identificador.",
            description = "Consulta el detalle de una tarifa por millar por su identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tarifa por millar encontrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ThousandRateSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "TarifaMillarDetalle",
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
                                        "thousandRateId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Color básico",
                                        "colorCategory": "1 COLOR",
                                        "thousandUnit": 1000,
                                        "price": 17500.00,
                                        "state": true,
                                        "minThresholdUnits": 600,
                                        "minThousand": 500.00,
                                        "decimalThreshold": 0.20,
                                        "gripperFlipPrice": 20000.00,
                                        "squareFlipPrice": 20000.00,
                                        "isDefault": false,
                                        "creationDate": "2026-08-08"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Tarifa por millar no encontrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ThousandRateResponse>> get(
            @Parameter(description = "Identificador de la tarifa por millar", required = true,
                    example = "814ad646-c4fe-42fa-9f13-4a44823e6bee")
            @PathVariable String thousandRateId,
            HttpServletRequest httpRequest
    ) {
        ThousandRate thousandRate = getThousandRateByIdUseCase.execute(thousandRateId);
        return responseFactory.success(httpRequest, HttpStatus.OK, ThousandRateResponse.from(thousandRate));
    }

    private static CreateThousandRateCommand toCreateCommand(CreateThousandRateRequest request) {
        return new CreateThousandRateCommand(
                request.companyId(),
                request.name(),
                request.colorCategory(),
                request.thousandUnit(),
                request.price(),
                request.state(),
                request.minThresholdUnits(),
                request.minThousand(),
                request.decimalThreshold(),
                request.gripperFlipPrice(),
                request.squareFlipPrice(),
                request.isDefault()
        );
    }

    private static UpdateThousandRateCommand toUpdateCommand(
            String thousandRateId,
            UpdateThousandRateRequest request
    ) {
        return new UpdateThousandRateCommand(
                thousandRateId,
                request.name(),
                request.colorCategory(),
                request.thousandUnit(),
                request.price(),
                Boolean.TRUE.equals(request.state()),
                request.minThresholdUnits(),
                request.minThousand(),
                request.decimalThreshold(),
                request.gripperFlipPrice(),
                request.squareFlipPrice(),
                Boolean.TRUE.equals(request.isDefault())
        );
    }
}
