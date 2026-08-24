package com.inkcore.infrastructure.in.rest.platetypes;

import com.inkcore.application.platetype.usecase.CreatePlateTypeCommand;
import com.inkcore.application.platetype.usecase.CreatePlateTypeUseCase;
import com.inkcore.application.platetype.usecase.GetPlateTypeByIdUseCase;
import com.inkcore.application.platetype.usecase.ListPlateTypesUseCase;
import com.inkcore.application.platetype.usecase.UpdatePlateTypeCommand;
import com.inkcore.application.platetype.usecase.UpdatePlateTypeUseCase;
import com.inkcore.domain.platetype.model.PlateType;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.PlateTypeListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.PlateTypeSuccessEnvelope;
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
@RequestMapping("/api/v1/plate-types")
@Tag(name = "Tipos de plancha", description = "Catálogo de tipos de plancha")
@SecurityRequirement(name = "bearerAuth")
public class PlateTypeController {

    private final CreatePlateTypeUseCase createPlateTypeUseCase;
    private final UpdatePlateTypeUseCase updatePlateTypeUseCase;
    private final ListPlateTypesUseCase listPlateTypesUseCase;
    private final GetPlateTypeByIdUseCase getPlateTypeByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public PlateTypeController(
            CreatePlateTypeUseCase createPlateTypeUseCase,
            UpdatePlateTypeUseCase updatePlateTypeUseCase,
            ListPlateTypesUseCase listPlateTypesUseCase,
            GetPlateTypeByIdUseCase getPlateTypeByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createPlateTypeUseCase = createPlateTypeUseCase;
        this.updatePlateTypeUseCase = updatePlateTypeUseCase;
        this.listPlateTypesUseCase = listPlateTypesUseCase;
        this.getPlateTypeByIdUseCase = getPlateTypeByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerPlateType",
            summary = "Crea un tipo de plancha nuevo.",
            description = "Crea un tipo de plancha (formulario Nuevo tipo de plancha). Nombre único por empresa. Sin delete físico: inactivar con state=false."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Tipo de plancha creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PlateTypeSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "TipoPlanchaCreado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Plate type created"
                                      },
                                      "timestamp": "2026-08-04T12:00:00Z",
                                      "data": {
                                        "plateTypeId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Plancha estándar",
                                        "width": 10.00,
                                        "height": 5.00,
                                        "unit": "cm",
                                        "value": 185000.00,
                                        "state": true,
                                        "creationDate": "2026-08-04"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PlateTypeResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nuevo tipo de plancha",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreatePlateTypeRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoTipoPlancha",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "name": "Plancha estándar",
                                              "width": 10.00,
                                              "height": 5.00,
                                              "unit": "cm",
                                              "value": 185000.00,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreatePlateTypeRequest request,
            HttpServletRequest httpRequest
    ) {
        PlateType created = createPlateTypeUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Plate type created",
                PlateTypeResponse.from(created)
        );
    }

    @PutMapping("/update/{plateTypeId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updatePlateType",
            summary = "Actualiza los datos de un tipo de plancha existente.",
            description = "Actualiza nombre, medidas, unidad, valor COP y estado. No cambia companyId. Nombre único por empresa."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tipo de plancha actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PlateTypeSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "TipoPlanchaActualizado",
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
                                        "plateTypeId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Plancha estándar",
                                        "width": 10.00,
                                        "height": 5.00,
                                        "unit": "cm",
                                        "value": 190000.00,
                                        "state": true,
                                        "creationDate": "2026-08-04"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PlateTypeResponse>> update(
            @Parameter(description = "Identificador del tipo de plancha", required = true,
                    example = "814ad646-c4fe-42fa-9f13-4a44823e6bee")
            @PathVariable String plateTypeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload de actualización del tipo de plancha",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdatePlateTypeRequest.class),
                            examples = @ExampleObject(
                                    name = "ActualizarTipoPlancha",
                                    value = """
                                            {
                                              "name": "Plancha estándar",
                                              "width": 10.00,
                                              "height": 5.00,
                                              "unit": "cm",
                                              "value": 190000.00,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdatePlateTypeRequest request,
            HttpServletRequest httpRequest
    ) {
        PlateType updated = updatePlateTypeUseCase.execute(toUpdateCommand(plateTypeId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, PlateTypeResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listPlateTypes",
            summary = "Obtiene el listado paginado de tipos de plancha.",
            description = "Listado paginado. Filtros opcionales: companyId y state."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de tipos de plancha",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PlateTypeListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ListadoTiposPlancha",
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
                                            "plateTypeId": "814ad646-c4fe-42fa-9f13-4a44823e6bee",
                                            "companyId": "company-seed-001",
                                            "name": "Plancha estándar",
                                            "width": 10.00,
                                            "height": 5.00,
                                            "unit": "cm",
                                            "value": 185000.00,
                                            "state": true,
                                            "creationDate": "2026-08-04"
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
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<PlateTypeResponse>>> list(
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
        PageResponse<PlateTypeResponse> data = PageResponse.from(
                listPlateTypesUseCase.execute(companyId, state, PageQuery.of(page, size)),
                PlateTypeResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{plateTypeId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getPlateType",
            summary = "Consulta el detalle de un tipo de plancha por su identificador.",
            description = "Consulta el detalle de un tipo de plancha por su identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tipo de plancha encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PlateTypeSuccessEnvelope.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Tipo de plancha no encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<PlateTypeResponse>> get(
            @Parameter(description = "Identificador del tipo de plancha", required = true,
                    example = "814ad646-c4fe-42fa-9f13-4a44823e6bee")
            @PathVariable String plateTypeId,
            HttpServletRequest httpRequest
    ) {
        PlateType plateType = getPlateTypeByIdUseCase.execute(plateTypeId);
        return responseFactory.success(httpRequest, HttpStatus.OK, PlateTypeResponse.from(plateType));
    }

    private static CreatePlateTypeCommand toCreateCommand(CreatePlateTypeRequest request) {
        return new CreatePlateTypeCommand(
                request.companyId(),
                request.name(),
                request.width(),
                request.height(),
                request.unit(),
                request.value(),
                request.state()
        );
    }

    private static UpdatePlateTypeCommand toUpdateCommand(String plateTypeId, UpdatePlateTypeRequest request) {
        return new UpdatePlateTypeCommand(
                plateTypeId,
                request.name(),
                request.width(),
                request.height(),
                request.unit(),
                request.value(),
                Boolean.TRUE.equals(request.state())
        );
    }
}
