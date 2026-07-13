package com.indicore.infrastructure.in.rest.auth;

import com.indicore.application.user.usecase.LoginResult;
import com.indicore.application.user.usecase.LoginUserCommand;
import com.indicore.application.user.usecase.LoginUserUseCase;
import com.indicore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.indicore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.indicore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.indicore.infrastructure.in.rest.openapi.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Credenciales y emisión de tokens JWT para el resto de la API")
public class AuthController {

    private final LoginUserUseCase loginUserUseCase;
    private final ApiResponseFactory responseFactory;

    public AuthController(LoginUserUseCase loginUserUseCase, ApiResponseFactory responseFactory) {
        this.loginUserUseCase = loginUserUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/login")
    @Operation(
            operationId = "login",
            summary = "Iniciar sesión",
            description = "Autentica con correo y contraseña. Respuesta: access token JWT (HS256) con claims sub, tv (token_version), roles y expiración configurada."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Autenticación correcta",
            content = @Content(schema = @Schema(implementation = ApiSuccessEnvelope.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Credenciales inválidas",
            content = @Content(schema = @Schema(implementation = ApiErrorEnvelope.class))
    )
    @ApiErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        LoginResult result = loginUserUseCase.execute(new LoginUserCommand(request.mail(), request.password()));
        LoginResponse body = new LoginResponse(result.accessToken(), result.tokenType(), result.expiresInSeconds());
        return responseFactory.success(httpRequest, HttpStatus.OK, body);
    }
}