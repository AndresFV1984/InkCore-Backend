package com.inkcore.infrastructure.in.rest.bankaccounts;

import com.inkcore.application.bankaccount.usecase.CreateBankAccountCommand;
import com.inkcore.application.bankaccount.usecase.CreateBankAccountUseCase;
import com.inkcore.application.bankaccount.usecase.GetBankAccountByIdUseCase;
import com.inkcore.application.bankaccount.usecase.ListBankAccountsUseCase;
import com.inkcore.application.bankaccount.usecase.UpdateBankAccountCommand;
import com.inkcore.application.bankaccount.usecase.UpdateBankAccountUseCase;
import com.inkcore.domain.bankaccount.model.BankAccount;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.BankAccountListSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.BankAccountSuccessEnvelope;
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
@RequestMapping("/api/v1/bank-accounts")
@Tag(name = "Cuentas bancarias", description = "Gestión de cuentas bancarias")
@SecurityRequirement(name = "bearerAuth")
public class BankAccountController {

    private final CreateBankAccountUseCase createBankAccountUseCase;
    private final UpdateBankAccountUseCase updateBankAccountUseCase;
    private final ListBankAccountsUseCase listBankAccountsUseCase;
    private final GetBankAccountByIdUseCase getBankAccountByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public BankAccountController(
            CreateBankAccountUseCase createBankAccountUseCase,
            UpdateBankAccountUseCase updateBankAccountUseCase,
            ListBankAccountsUseCase listBankAccountsUseCase,
            GetBankAccountByIdUseCase getBankAccountByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createBankAccountUseCase = createBankAccountUseCase;
        this.updateBankAccountUseCase = updateBankAccountUseCase;
        this.listBankAccountsUseCase = listBankAccountsUseCase;
        this.getBankAccountByIdUseCase = getBankAccountByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "registerBankAccount",
            summary = "Crea una cuenta bancaria nueva.",
            description = "Crea una cuenta bancaria nueva."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Cuenta bancaria creada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BankAccountSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "CuentaCreada",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "Bank account created"
                                      },
                                      "timestamp": "2026-07-28T12:00:00Z",
                                      "data": {
                                        "accountId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "bankName": "Bancolombia",
                                        "accountType": "Corriente",
                                        "accountNumber": "12345678901",
                                        "holderName": "InkCore S.A.S.",
                                        "holderNit": "900.000.000-1",
                                        "includeInPdf": true,
                                        "isPrimary": false,
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
    public ResponseEntity<ApiSuccessEnvelope<BankAccountResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nueva cuenta bancaria",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateBankAccountRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevaCuentaBancaria",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "bankName": "Bancolombia",
                                              "accountType": "Corriente",
                                              "accountNumber": "12345678901",
                                              "holderName": "InkCore S.A.S.",
                                              "holderNit": "900.000.000-1",
                                              "includeInPdf": true,
                                              "isPrimary": false,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateBankAccountRequest request,
            HttpServletRequest httpRequest
    ) {
        BankAccount created = createBankAccountUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Bank account created",
                BankAccountResponse.from(created)
        );
    }

    @PutMapping("/update/{accountId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "updateBankAccount",
            summary = "Actualiza los datos de una cuenta bancaria existente.",
            description = "Actualiza los datos de una cuenta bancaria existente."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cuenta bancaria actualizada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BankAccountSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "CuentaActualizada",
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
                                        "accountId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "bankName": "Davivienda",
                                        "accountType": "Ahorros",
                                        "accountNumber": "12345678901",
                                        "holderName": "InkCore S.A.S.",
                                        "holderNit": "900.000.000-1",
                                        "includeInPdf": true,
                                        "isPrimary": true,
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
    public ResponseEntity<ApiSuccessEnvelope<BankAccountResponse>> update(
            @Parameter(
                    description = "Identificador de la cuenta bancaria",
                    required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            )
            @PathVariable String accountId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos a actualizar (sin companyId; se conserva el de BD)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateBankAccountRequest.class),
                            examples = @ExampleObject(
                                    name = "ActualizarCuentaBancaria",
                                    value = """
                                            {
                                              "bankName": "Davivienda",
                                              "accountType": "Ahorros",
                                              "accountNumber": "12345678901",
                                              "holderName": "InkCore S.A.S.",
                                              "holderNit": "900.000.000-1",
                                              "includeInPdf": true,
                                              "isPrimary": true,
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateBankAccountRequest request,
            HttpServletRequest httpRequest
    ) {
        BankAccount updated = updateBankAccountUseCase.execute(toUpdateCommand(accountId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, BankAccountResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "listBankAccounts",
            summary = "Obtiene el listado paginado de cuentas bancarias.",
            description = "Obtiene el listado paginado de cuentas bancarias."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de cuentas bancarias",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BankAccountListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "CuentasPaginadas",
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
                                            "accountId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                            "companyId": "company-seed-001",
                                            "bankName": "Bancolombia",
                                            "accountType": "Corriente",
                                            "accountNumber": "12345678901",
                                            "holderName": "InkCore S.A.S.",
                                            "holderNit": "900.000.000-1",
                                            "includeInPdf": true,
                                            "isPrimary": true,
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
    public ResponseEntity<ApiSuccessEnvelope<PageResponse<BankAccountResponse>>> list(
            @Parameter(description = "Filtro por empresa", example = "company-seed-001")
            @RequestParam(required = false) String companyId,
            @Parameter(description = "Filtro por estado: true=activas, false=inactivas, omitir=todas")
            @RequestParam(required = false) Boolean state,
            @Parameter(description = "Página 0-based", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Tamaño de página (máx 100)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,
            HttpServletRequest httpRequest
    ) {
        PageResponse<BankAccountResponse> data = PageResponse.from(
                listBankAccountsUseCase.execute(companyId, state, PageQuery.of(page, size)),
                BankAccountResponse::from
        );
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/get/{accountId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "getBankAccount",
            summary = "Consulta el detalle de una cuenta bancaria por su identificador.",
            description = "Consulta el detalle de una cuenta bancaria por su identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cuenta bancaria encontrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BankAccountSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "CuentaDetalle",
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
                                        "accountId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "bankName": "Bancolombia",
                                        "accountType": "Corriente",
                                        "accountNumber": "12345678901",
                                        "holderName": "InkCore S.A.S.",
                                        "holderNit": "900.000.000-1",
                                        "includeInPdf": true,
                                        "isPrimary": false,
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
            description = "Cuenta bancaria no encontrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<BankAccountResponse>> get(
            @Parameter(
                    description = "Identificador de la cuenta bancaria",
                    required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            )
            @PathVariable String accountId,
            HttpServletRequest httpRequest
    ) {
        BankAccount account = getBankAccountByIdUseCase.execute(accountId);
        return responseFactory.success(httpRequest, HttpStatus.OK, BankAccountResponse.from(account));
    }

    private static CreateBankAccountCommand toCreateCommand(CreateBankAccountRequest request) {
        return new CreateBankAccountCommand(
                request.companyId(),
                request.bankName(),
                request.accountType(),
                request.accountNumber(),
                request.holderName(),
                request.holderNit(),
                request.includeInPdf(),
                request.isPrimary(),
                request.state()
        );
    }

    private static UpdateBankAccountCommand toUpdateCommand(String accountId, UpdateBankAccountRequest request) {
        return new UpdateBankAccountCommand(
                accountId,
                request.bankName(),
                request.accountType(),
                request.accountNumber(),
                request.holderName(),
                request.holderNit(),
                Boolean.TRUE.equals(request.includeInPdf()),
                Boolean.TRUE.equals(request.isPrimary()),
                Boolean.TRUE.equals(request.state())
        );
    }
}
