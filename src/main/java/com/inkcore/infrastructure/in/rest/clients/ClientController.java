package com.inkcore.infrastructure.in.rest.clients;

import com.inkcore.application.client.usecase.CreateClientCommand;
import com.inkcore.application.client.usecase.CreateClientUseCase;
import com.inkcore.application.client.usecase.GetClientByIdUseCase;
import com.inkcore.application.client.usecase.ListClientsUseCase;
import com.inkcore.application.client.usecase.UpdateClientCommand;
import com.inkcore.application.client.usecase.UpdateClientUseCase;
import com.inkcore.domain.client.model.Client;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ClientListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ClientSuccessEnvelope;
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
@RequestMapping("/api/v1/clients")
@Tag(
        name = "Clientes",
        description = """
                Alta (`/register`), listado (`/list`), detalle (`/get/{clientId}`) y actualización (`/update/{clientId}`).
                Requiere JWT Bearer. Respuesta con `documentType` anidado.
                """
)
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final CreateClientUseCase createClientUseCase;
    private final UpdateClientUseCase updateClientUseCase;
    private final ListClientsUseCase listClientsUseCase;
    private final GetClientByIdUseCase getClientByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public ClientController(
            CreateClientUseCase createClientUseCase,
            UpdateClientUseCase updateClientUseCase,
            ListClientsUseCase listClientsUseCase,
            GetClientByIdUseCase getClientByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createClientUseCase = createClientUseCase;
        this.updateClientUseCase = updateClientUseCase;
        this.listClientsUseCase = listClientsUseCase;
        this.getClientByIdUseCase = getClientByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "registerClient",
            summary = "Registrar cliente",
            description = """
                    Crea un cliente (formulario Nuevo cliente).
                    Obligatorios: companyId, name, department, city.
                    Opcionales: documentType (CC, CE, TI, PA, NIT), identification, address, phone, email, contactPerson, state.
                    Si se envía identification y ya existe en la misma empresa → 409 CONFLICT.
                    Respuesta con documentType anidado `{ documentType, identificationNumber }`.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Cliente creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ClientSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ClienteCreado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Client created"
                                      },
                                      "timestamp": "2026-07-22T12:00:00Z",
                                      "data": {
                                        "clientId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Comercializadora ABC S.A.S.",
                                        "documentType": {
                                          "documentType": "NIT",
                                          "identificationNumber": "900123456-1"
                                        },
                                        "department": {
                                          "department": "Antioquia",
                                          "city": "Medellín"
                                        },
                                        "address": "Calle 10 # 20-30",
                                        "phone": "604 123 4567",
                                        "email": "correo@empresa.com",
                                        "contactPerson": "Ana Gómez",
                                        "state": true,
                                        "creationDate": "2026-07-22"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ClientResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nuevo cliente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateClientRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoCliente",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "name": "Comercializadora ABC S.A.S.",
                                              "documentType": "NIT",
                                              "identification": "900123456-1",
                                              "department": "Antioquia",
                                              "city": "Medellín",
                                              "address": "Calle 10 # 20-30",
                                              "phone": "604 123 4567",
                                              "email": "correo@empresa.com",
                                              "contactPerson": "Ana Gómez",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateClientRequest request,
            HttpServletRequest httpRequest
    ) {
        Client created = createClientUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Client created",
                ClientResponse.from(created)
        );
    }

    @PutMapping("/update/{clientId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "updateClient",
            summary = "Actualizar cliente",
            description = """
                    Actualiza datos del cliente. `clientId` va en la ruta; el body no incluye `companyId`.
                    Obligatorios: name, department, city, state.
                    Opcionales: documentType (CC, CE, TI, PA, NIT), identification, address, phone, email, contactPerson.
                    Si identification ya existe en la misma empresa → 409 CONFLICT.
                    Respuesta con documentType anidado `{ documentType, identificationNumber }`.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cliente actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ClientSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ClienteActualizado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-22T12:05:00Z",
                                      "data": {
                                        "clientId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Comercializadora ABC S.A.S.",
                                        "documentType": {
                                          "documentType": "NIT",
                                          "identificationNumber": "900123456-1"
                                        },
                                        "department": {
                                          "department": "Antioquia",
                                          "city": "Envigado"
                                        },
                                        "address": "Carrera 40 # 15-20",
                                        "phone": "604 987 6543",
                                        "email": "contacto@abc.com",
                                        "contactPerson": "Luis Pérez",
                                        "state": true,
                                        "creationDate": "2026-07-22"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ClientResponse>> update(
            @Parameter(
                    description = "Identificador del cliente",
                    required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            )
            @PathVariable String clientId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos a actualizar (sin companyId; se conserva el de BD)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateClientRequest.class),
                            examples = @ExampleObject(
                                    name = "ActualizarCliente",
                                    summary = "Actualización completa con documento",
                                    value = """
                                            {
                                              "name": "Comercializadora ABC S.A.S.",
                                              "documentType": "NIT",
                                              "identification": "900123456-1",
                                              "department": "Antioquia",
                                              "city": "Envigado",
                                              "address": "Carrera 40 # 15-20",
                                              "phone": "604 987 6543",
                                              "email": "contacto@abc.com",
                                              "contactPerson": "Luis Pérez",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateClientRequest request,
            HttpServletRequest httpRequest
    ) {
        Client updated = updateClientUseCase.execute(toUpdateCommand(clientId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, ClientResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "listClients",
            summary = "Listar clientes",
            description = """
                    Query opcionales: `companyId`, `state` (true=activos, false=inactivos, ausente=todos).
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de clientes",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ClientListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "Clientes",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-22T12:00:00Z",
                                      "data": [
                                        {
                                          "clientId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                          "companyId": "company-seed-001",
                                          "name": "Comercializadora ABC S.A.S.",
                                          "documentType": {
                                            "documentType": "NIT",
                                            "identificationNumber": "900123456-1"
                                          },
                                          "department": {
                                            "department": "Antioquia",
                                            "city": "Medellín"
                                          },
                                          "address": "Calle 10 # 20-30",
                                          "phone": "604 123 4567",
                                          "email": "correo@empresa.com",
                                          "contactPerson": "Ana Gómez",
                                          "state": true,
                                          "creationDate": "2026-07-22"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<ClientResponse>>> list(
            @Parameter(description = "Filtro por empresa", example = "company-seed-001")
            @RequestParam(required = false) String companyId,
            @Parameter(description = "Filtro por estado: true=activos, false=inactivos, omitir=todos")
            @RequestParam(required = false) Boolean state,
            HttpServletRequest httpRequest
    ) {
        List<ClientResponse> data = listClientsUseCase.execute(companyId, state).stream()
                .map(ClientResponse::from)
                .toList();
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{clientId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "getClient",
            summary = "Consultar cliente por ID"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cliente encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ClientSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "ClienteDetalle",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-22T12:00:00Z",
                                      "data": {
                                        "clientId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "name": "Comercializadora ABC S.A.S.",
                                        "documentType": {
                                          "documentType": "NIT",
                                          "identificationNumber": "900123456-1"
                                        },
                                        "department": {
                                          "department": "Antioquia",
                                          "city": "Medellín"
                                        },
                                        "address": "Calle 10 # 20-30",
                                        "phone": "604 123 4567",
                                        "email": "correo@empresa.com",
                                        "contactPerson": "Ana Gómez",
                                        "state": true,
                                        "creationDate": "2026-07-22"
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Cliente no encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<ClientResponse>> get(
            @Parameter(
                    description = "Identificador del cliente",
                    required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            )
            @PathVariable String clientId,
            HttpServletRequest httpRequest
    ) {
        Client client = getClientByIdUseCase.execute(clientId);
        return responseFactory.success(httpRequest, HttpStatus.OK, ClientResponse.from(client));
    }

    private static CreateClientCommand toCreateCommand(CreateClientRequest request) {
        return new CreateClientCommand(
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

    private static UpdateClientCommand toUpdateCommand(String clientId, UpdateClientRequest request) {
        return new UpdateClientCommand(
                clientId,
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
