package com.inkcore.infrastructure.in.rest.suppliers;

import com.inkcore.application.supplier.usecase.CreateSupplierCommand;
import com.inkcore.application.supplier.usecase.CreateSupplierUseCase;
import com.inkcore.application.supplier.usecase.GetSupplierByIdUseCase;
import com.inkcore.application.supplier.usecase.ListSuppliersUseCase;
import com.inkcore.application.supplier.usecase.UpdateSupplierCommand;
import com.inkcore.application.supplier.usecase.UpdateSupplierUseCase;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.supplier.model.Supplier;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.SupplierListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.SupplierSuccessEnvelope;
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
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Proveedores", description = "Gestión de proveedores")
@SecurityRequirement(name = "bearerAuth")
public class SupplierController {

    private final CreateSupplierUseCase createSupplierUseCase;
    private final UpdateSupplierUseCase updateSupplierUseCase;
    private final ListSuppliersUseCase listSuppliersUseCase;
    private final GetSupplierByIdUseCase getSupplierByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public SupplierController(
            CreateSupplierUseCase createSupplierUseCase,
            UpdateSupplierUseCase updateSupplierUseCase,
            ListSuppliersUseCase listSuppliersUseCase,
            GetSupplierByIdUseCase getSupplierByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createSupplierUseCase = createSupplierUseCase;
        this.updateSupplierUseCase = updateSupplierUseCase;
        this.listSuppliersUseCase = listSuppliersUseCase;
        this.getSupplierByIdUseCase = getSupplierByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "registerSupplier",
            summary = "Crea un proveedor nuevo.",
            description = "Crea un proveedor nuevo."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Proveedor creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SupplierSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ProveedorCreado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Supplier created"
                                      },
                                      "timestamp": "2026-08-27T12:00:00Z",
                                      "data": {
                                        "supplierId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Papeles del Norte S.A.S.",
                                        "documentType": {
                                          "documentType": "NIT",
                                          "identificationNumber": "900987654-3"
                                        },
                                        "department": {
                                          "department": "Antioquia",
                                          "city": "Medellín"
                                        },
                                        "address": "Carrera 50 # 25-10",
                                        "phone": "604 555 1234",
                                        "email": "ventas@papelesdelnorte.com",
                                        "contactPerson": "María López",
                                        "state": true,
                                        "creationDate": "2026-08-27"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<SupplierResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nuevo proveedor",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateSupplierRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoProveedor",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "name": "Papeles del Norte S.A.S.",
                                              "documentType": "NIT",
                                              "identification": "900987654-3",
                                              "department": "Antioquia",
                                              "city": "Medellín",
                                              "address": "Carrera 50 # 25-10",
                                              "phone": "604 555 1234",
                                              "email": "ventas@papelesdelnorte.com",
                                              "contactPerson": "María López",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateSupplierRequest request,
            HttpServletRequest httpRequest
    ) {
        Supplier created = createSupplierUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Supplier created",
                SupplierResponse.from(created)
        );
    }

    @PutMapping("/update/{supplierId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "updateSupplier",
            summary = "Actualiza los datos de un proveedor existente.",
            description = "Actualiza los datos de un proveedor existente."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Proveedor actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SupplierSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ProveedorActualizado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-08-27T12:05:00Z",
                                      "data": {
                                        "supplierId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Papeles del Norte S.A.S.",
                                        "documentType": {
                                          "documentType": "NIT",
                                          "identificationNumber": "900987654-3"
                                        },
                                        "department": {
                                          "department": "Antioquia",
                                          "city": "Envigado"
                                        },
                                        "address": "Calle 30 # 45-20",
                                        "phone": "604 555 9876",
                                        "email": "compras@papelesdelnorte.com",
                                        "contactPerson": "Carlos Ruiz",
                                        "state": true,
                                        "creationDate": "2026-08-27"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<SupplierResponse>> update(
            @Parameter(
                    description = "Identificador del proveedor",
                    required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            )
            @PathVariable String supplierId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos a actualizar (sin companyId; se conserva el de BD)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateSupplierRequest.class),
                            examples = @ExampleObject(
                                    name = "ActualizarProveedor",
                                    value = """
                                            {
                                              "name": "Papeles del Norte S.A.S.",
                                              "documentType": "NIT",
                                              "identification": "900987654-3",
                                              "department": "Antioquia",
                                              "city": "Envigado",
                                              "address": "Calle 30 # 45-20",
                                              "phone": "604 555 9876",
                                              "email": "compras@papelesdelnorte.com",
                                              "contactPerson": "Carlos Ruiz",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateSupplierRequest request,
            HttpServletRequest httpRequest
    ) {
        Supplier updated = updateSupplierUseCase.execute(toUpdateCommand(supplierId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, SupplierResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "listSuppliers",
            summary = "Obtiene el listado paginado de proveedores.",
            description = "Obtiene el listado paginado de proveedores."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de proveedores",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SupplierListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ProveedoresPaginados",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-08-27T12:00:00Z",
                                      "data": {
                                        "content": [
                                          {
                                            "supplierId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                            "companyId": "company-seed-001",
                                            "name": "Papeles del Norte S.A.S.",
                                            "documentType": {
                                              "documentType": "NIT",
                                              "identificationNumber": "900987654-3"
                                            },
                                            "department": {
                                              "department": "Antioquia",
                                              "city": "Medellín"
                                            },
                                            "address": "Carrera 50 # 25-10",
                                            "phone": "604 555 1234",
                                            "email": "ventas@papelesdelnorte.com",
                                            "contactPerson": "María López",
                                            "state": true,
                                            "creationDate": "2026-08-27"
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
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<SupplierResponse>>> list(
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
        PageResponse<SupplierResponse> data = PageResponse.from(
                listSuppliersUseCase.execute(companyId, state, PageQuery.of(page, size)),
                SupplierResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{supplierId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getSupplier",
            summary = "Consulta el detalle de un proveedor por su identificador.",
            description = "Consulta el detalle de un proveedor por su identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Proveedor encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SupplierSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ProveedorDetalle",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-08-27T12:00:00Z",
                                      "data": {
                                        "supplierId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Papeles del Norte S.A.S.",
                                        "documentType": {
                                          "documentType": "NIT",
                                          "identificationNumber": "900987654-3"
                                        },
                                        "department": {
                                          "department": "Antioquia",
                                          "city": "Medellín"
                                        },
                                        "address": "Carrera 50 # 25-10",
                                        "phone": "604 555 1234",
                                        "email": "ventas@papelesdelnorte.com",
                                        "contactPerson": "María López",
                                        "state": true,
                                        "creationDate": "2026-08-27"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Proveedor no encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<SupplierResponse>> get(
            @Parameter(
                    description = "Identificador del proveedor",
                    required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            )
            @PathVariable String supplierId,
            HttpServletRequest httpRequest
    ) {
        Supplier supplier = getSupplierByIdUseCase.execute(supplierId);
        return responseFactory.success(httpRequest, HttpStatus.OK, SupplierResponse.from(supplier));
    }

    private static CreateSupplierCommand toCreateCommand(CreateSupplierRequest request) {
        return new CreateSupplierCommand(
                request.companyId(),
                request.name(),
                request.documentType(),
                request.identification(),
                request.department(),
                request.city(),
                request.address(),
                request.phone(),
                request.email(),
                request.contactPerson(),
                request.state()
        );
    }

    private static UpdateSupplierCommand toUpdateCommand(String supplierId, UpdateSupplierRequest request) {
        return new UpdateSupplierCommand(
                supplierId,
                request.name(),
                request.documentType(),
                request.identification(),
                request.department(),
                request.city(),
                request.address(),
                request.phone(),
                request.email(),
                request.contactPerson(),
                Boolean.TRUE.equals(request.state())
        );
    }
}
