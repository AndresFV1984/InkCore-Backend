package com.inkcore.infrastructure.in.rest.auth;

import com.inkcore.application.user.usecase.LoginResult;
import com.inkcore.application.user.usecase.LoginUserCommand;
import com.inkcore.application.user.usecase.LoginUserUseCase;
import com.inkcore.application.user.usecase.RefreshAccessTokenUseCase;
import com.inkcore.application.user.usecase.RefreshResult;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.users.LoginRequest;
import com.inkcore.infrastructure.in.rest.users.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Login y renovación de tokens (endpoints públicos)")
@SecurityRequirements
public class AuthController {

    private final LoginUserUseCase loginUserUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final ApiResponseFactory responseFactory;

    public AuthController(
            LoginUserUseCase loginUserUseCase,
            RefreshAccessTokenUseCase refreshAccessTokenUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.loginUserUseCase = loginUserUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/login")
    @Operation(
            operationId = "authLogin",
            summary = "Iniciar sesión",
            description = """
                    Alias público de `POST /api/v1/users/login`.
                    No requiere Authorization. Emite access JWT + refresh opaco en headers del envelope.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Login correcto",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "LoginOk",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Login successful",
                                        "token": "Bearer eyJhbGciOiJIUzI1NiJ9...",
                                        "tokenAccesExpira": 3600,
                                        "refreshToken": "opaque-refresh-token",
                                        "tokenRefresExpira": 1209600
                                      },
                                      "data": {
                                        "userId": "seed-cfg-9f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c",
                                        "companyId": "company-seed-001",
                                        "documentType": {
                                          "documentType": "NIT",
                                          "identificationNumber": "9001234567"
                                        },
                                        "name": "Administrador InkCore",
                                        "state": true,
                                        "roles": [
                                          {
                                            "role": "ADMINISTRADOR",
                                            "permissions": ["dashboard.view", "orders.view"]
                                          }
                                        ]
                                      }
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Credenciales inválidas, cuenta bloqueada o contraseña expirada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<LoginResponse>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Credenciales de acceso",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "AdminSeed",
                                    summary = "Usuario semilla administrador",
                                    value = """
                                            {
                                              "mail": "admin@indicolors.com",
                                              "password": "Indicore2026!"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        LoginResult result = loginUserUseCase.execute(new LoginUserCommand(request.mail(), request.password()));
        return responseFactory.okWithTokens(
                httpRequest,
                LoginResponse.from(result),
                result.accessToken(),
                result.accessExpiresInSeconds(),
                result.refreshToken(),
                result.refreshExpiresInSeconds(),
                "OK",
                "Login successful"
        );
    }

    @PostMapping("/refresh")
    @Operation(
            operationId = "refresh",
            summary = "Renovar access token",
            description = """
                    Emite un nuevo access JWT a partir de un refresh token opaco válido.
                    Público (sin Authorization). Si `rotate-on-refresh=true`, rota el refresh.
                    Tokens van en headers del envelope; `data` es null.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Tokens renovados",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Refresh token inválido, expirado o revocado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<Object>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        RefreshResult result = refreshAccessTokenUseCase.refresh(request.refreshToken());
        return responseFactory.okWithTokens(
                httpRequest,
                null,
                result.accessToken(),
                result.tokenAccesExpira(),
                result.refreshToken(),
                result.tokenRefresExpira(),
                "OK",
                "Token refreshed"
        );
    }
}
