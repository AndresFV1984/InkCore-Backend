package com.inkcore.infrastructure.in.rest.sellers;

import com.inkcore.application.seller.usecase.CreateSellerCommand;
import com.inkcore.application.seller.usecase.CreateSellerUseCase;
import com.inkcore.application.seller.usecase.GetSellerByIdUseCase;
import com.inkcore.application.seller.usecase.ListSellersUseCase;
import com.inkcore.application.seller.usecase.UpdateSellerCommand;
import com.inkcore.application.seller.usecase.UpdateSellerUseCase;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import com.inkcore.domain.seller.model.Seller;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.SellerListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.SellerSuccessEnvelope;
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
@RequestMapping("/api/v1/sellers")
@Tag(name = "Vendedores", description = "Gestión de vendedores")
@SecurityRequirement(name = "bearerAuth")
public class SellerController {

    private final CreateSellerUseCase createSellerUseCase;
    private final UpdateSellerUseCase updateSellerUseCase;
    private final ListSellersUseCase listSellersUseCase;
    private final GetSellerByIdUseCase getSellerByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public SellerController(
            CreateSellerUseCase createSellerUseCase,
            UpdateSellerUseCase updateSellerUseCase,
            ListSellersUseCase listSellersUseCase,
            GetSellerByIdUseCase getSellerByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createSellerUseCase = createSellerUseCase;
        this.updateSellerUseCase = updateSellerUseCase;
        this.listSellersUseCase = listSellersUseCase;
        this.getSellerByIdUseCase = getSellerByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "registerSeller",
            summary = "Crea un vendedor nuevo.",
            description = "Crea un vendedor nuevo."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Vendedor creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SellerSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "VendedorCreado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Seller created"
                                      },
                                      "timestamp": "2026-07-25T12:00:00Z",
                                      "data": {
                                        "sellerId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "fullName": "Carlos Andrés Gómez",
                                        "documentType": {
                                          "documentType": "CC",
                                          "identificationNumber": "1020304050"
                                        },
                                        "email": "vendedor@empresa.com",
                                        "phone": "300 123 4567",
                                        "department": {
                                          "department": "Antioquia",
                                          "city": "Medellín"
                                        },
                                        "address": "Calle 10 # 20-30",
                                        "state": true,
                                        "creationDate": "2026-07-25"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<SellerResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nuevo vendedor",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateSellerRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoVendedor",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "fullName": "Carlos Andrés Gómez",
                                              "documentType": "CC",
                                              "identification": "1020304050",
                                              "email": "vendedor@empresa.com",
                                              "phone": "300 123 4567",
                                              "department": "Antioquia",
                                              "city": "Medellín",
                                              "address": "Calle 10 # 20-30",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateSellerRequest request,
            HttpServletRequest httpRequest
    ) {
        Seller created = createSellerUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Seller created",
                SellerResponse.from(created)
        );
    }

    @PutMapping("/update/{sellerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "updateSeller",
            summary = "Actualiza los datos de un vendedor existente.",
            description = "Actualiza los datos de un vendedor existente."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Vendedor actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SellerSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "VendedorActualizado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-25T12:05:00Z",
                                      "data": {
                                        "sellerId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "fullName": "Carlos Andrés Gómez",
                                        "documentType": {
                                          "documentType": "CC",
                                          "identificationNumber": "1020304050"
                                        },
                                        "email": "vendedor@empresa.com",
                                        "phone": "300 987 6543",
                                        "department": {
                                          "department": "Antioquia",
                                          "city": "Envigado"
                                        },
                                        "address": "Carrera 40 # 15-20",
                                        "state": true,
                                        "creationDate": "2026-07-25"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<SellerResponse>> update(
            @Parameter(
                    description = "Identificador del vendedor",
                    required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            )
            @PathVariable String sellerId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos a actualizar (sin companyId; se conserva el de BD)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateSellerRequest.class),
                            examples = @ExampleObject(
                                    name = "ActualizarVendedor",
                                    value = """
                                            {
                                              "fullName": "Carlos Andrés Gómez",
                                              "documentType": "CC",
                                              "identification": "1020304050",
                                              "email": "vendedor@empresa.com",
                                              "phone": "300 987 6543",
                                              "department": "Antioquia",
                                              "city": "Envigado",
                                              "address": "Carrera 40 # 15-20",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateSellerRequest request,
            HttpServletRequest httpRequest
    ) {
        Seller updated = updateSellerUseCase.execute(toUpdateCommand(sellerId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, SellerResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "listSellers",
            summary = "Obtiene el listado paginado de vendedores.",
            description = "Obtiene el listado paginado de vendedores."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de vendedores",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SellerListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "VendedoresPaginados",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-25T12:00:00Z",
                                      "data": {
                                        "content": [
                                          {
                                            "sellerId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                            "companyId": "company-seed-001",
                                            "fullName": "Carlos Andrés Gómez",
                                            "documentType": {
                                              "documentType": "CC",
                                              "identificationNumber": "1020304050"
                                            },
                                            "email": "vendedor@empresa.com",
                                            "phone": "300 123 4567",
                                            "department": {
                                              "department": "Antioquia",
                                              "city": "Medellín"
                                            },
                                            "address": "Calle 10 # 20-30",
                                            "state": true,
                                            "creationDate": "2026-07-25"
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
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<SellerResponse>>> list(
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
        PageResponse<SellerResponse> data = PageResponse.from(
                listSellersUseCase.execute(companyId, state, PageQuery.of(page, size)),
                SellerResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{sellerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "getSeller",
            summary = "Consulta el detalle de un vendedor por su identificador.",
            description = "Consulta el detalle de un vendedor por su identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Vendedor encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SellerSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "VendedorDetalle",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-25T12:00:00Z",
                                      "data": {
                                        "sellerId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "fullName": "Carlos Andrés Gómez",
                                        "documentType": {
                                          "documentType": "CC",
                                          "identificationNumber": "1020304050"
                                        },
                                        "email": "vendedor@empresa.com",
                                        "phone": "300 123 4567",
                                        "department": {
                                          "department": "Antioquia",
                                          "city": "Medellín"
                                        },
                                        "address": "Calle 10 # 20-30",
                                        "state": true,
                                        "creationDate": "2026-07-25"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Vendedor no encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<SellerResponse>> get(
            @Parameter(
                    description = "Identificador del vendedor",
                    required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            )
            @PathVariable String sellerId,
            HttpServletRequest httpRequest
    ) {
        Seller seller = getSellerByIdUseCase.execute(sellerId);
        return responseFactory.success(httpRequest, HttpStatus.OK, SellerResponse.from(seller));
    }

    private static CreateSellerCommand toCreateCommand(CreateSellerRequest request) {
        return new CreateSellerCommand(
                request.companyId(),
                request.fullName(),
                request.documentType(),
                request.identification(),
                request.email(),
                request.phone(),
                request.department(),
                request.city(),
                request.address(),
                request.state()
        );
    }

    private static UpdateSellerCommand toUpdateCommand(String sellerId, UpdateSellerRequest request) {
        return new UpdateSellerCommand(
                sellerId,
                request.fullName(),
                request.documentType(),
                request.identification(),
                request.email(),
                request.phone(),
                request.department(),
                request.city(),
                request.address(),
                Boolean.TRUE.equals(request.state())
        );
    }
}
