package com.indicore.infrastructure.in.rest.clients;

import com.indicore.application.client.usecase.CreateClientCommand;
import com.indicore.application.client.usecase.CreateClientUseCase;
import com.indicore.application.client.usecase.GetClientByIdUseCase;
import com.indicore.application.client.usecase.ListClientsUseCase;
import com.indicore.domain.client.model.Client;
import com.indicore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.indicore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.indicore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.indicore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@Tag(name = "Clientes", description = "Directorio comercial: alta, listado y consulta de clientes")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class ClientController {

    private final CreateClientUseCase createClientUseCase;
    private final ListClientsUseCase listClientsUseCase;
    private final GetClientByIdUseCase getClientByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public ClientController(
            CreateClientUseCase createClientUseCase,
            ListClientsUseCase listClientsUseCase,
            GetClientByIdUseCase getClientByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createClientUseCase = createClientUseCase;
        this.listClientsUseCase = listClientsUseCase;
        this.getClientByIdUseCase = getClientByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @Operation(
            operationId = "registerClient",
            summary = "Registrar cliente",
            description = "Crea un cliente en el directorio (nombre, NIT/C.C., contacto, ciudad, direcciÃ³n). Requiere usuario autenticado."
    )
    public ResponseEntity<ApiSuccessEnvelope<ClientResponse>> register(
            @Valid @RequestBody CreateClientRequest request,
            HttpServletRequest httpRequest
    ) {
        Client created = createClientUseCase.execute(new CreateClientCommand(
                request.name(),
                request.nit(),
                request.phone(),
                request.city(),
                request.address(),
                request.email(),
                request.contact()
        ));
        return responseFactory.success(
                httpRequest,
                HttpStatus.CREATED,
                ClientResponse.from(created)
        );
    }

    @GetMapping("/list")
    @Operation(
            operationId = "listClients",
            summary = "Listar clientes",
            description = "Devuelve el listado completo de clientes registrados para uso en directorio y bÃºsquedas."
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<ClientResponse>>> list(HttpServletRequest httpRequest) {
        List<ClientResponse> data = listClientsUseCase.execute().stream()
                .map(ClientResponse::from)
                .toList();
        return responseFactory.success(httpRequest, HttpStatus.OK, data);
    }

    @GetMapping("/get/{id}")
    @Operation(
            operationId = "getClient",
            summary = "Consultar cliente por ID",
            description = "Obtiene los datos de un cliente existente a partir de su identificador Ãºnico (UUID)."
    )
    public ResponseEntity<ApiSuccessEnvelope<ClientResponse>> get(
            @PathVariable UUID id,
            HttpServletRequest httpRequest
    ) {
        Client client = getClientByIdUseCase.execute(id);
        return responseFactory.success(httpRequest, HttpStatus.OK, ClientResponse.from(client));
    }
}
