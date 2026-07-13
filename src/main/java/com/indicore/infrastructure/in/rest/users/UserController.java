package com.indicore.infrastructure.in.rest.users;

import com.indicore.application.user.usecase.CreateUserCommand;
import com.indicore.application.user.usecase.CreateUserUseCase;
import com.indicore.application.user.usecase.GetUserByIdUseCase;
import com.indicore.application.user.usecase.ListUsersUseCase;
import com.indicore.domain.user.model.User;
import com.indicore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.indicore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.indicore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.indicore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.indicore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.indicore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuarios", description = "Cuentas del sistema: registro (admin), listado, perfil y consulta por ID (esquema indicolors.users)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public UserController(
            CreateUserUseCase createUserUseCase,
            ListUsersUseCase listUsersUseCase,
            GetUserByIdUseCase getUserByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createUserUseCase = createUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "registerUser",
            summary = "Registrar usuario",
            description = "Crea una cuenta de usuario en el sistema (tabla indicolors.users). Requiere rol ADMINISTRADOR. La contraseña se almacena con hash."
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<UserResponse>> register(
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest
    ) {
        User created = createUserUseCase.execute(new CreateUserCommand(
                request.companyId(),
                request.identificationNumber(),
                request.documentType(),
                request.name(),
                request.mail(),
                request.contact(),
                request.address(),
                request.password(),
                request.roleId(),
                request.roleCode()
        ));
        return responseFactory.success(httpRequest, HttpStatus.CREATED, UserResponse.from(created));
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "listUsers",
            summary = "Listar usuarios",
            description = "Obtiene todos los usuarios registrados. Uso tÃ­pico: administraciÃ³n. Requiere rol ADMINISTRADOR."
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<UserResponse>>> list(HttpServletRequest httpRequest) {
        List<UserResponse> data = listUsersUseCase.execute().stream().map(UserResponse::from).toList();
        return responseFactory.success(httpRequest, HttpStatus.OK, data);
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "profileUser",
            summary = "Mi perfil",
            description = "Devuelve los datos del usuario asociado al token JWT actual (claim sub)."
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<UserResponse>> profile(Authentication authentication, HttpServletRequest httpRequest) {
        User user = getUserByIdUseCase.execute(authentication.getName());
        return responseFactory.success(httpRequest, HttpStatus.OK, UserResponse.from(user));
    }

    @GetMapping("/get/{userId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or authentication.name == #userId")
    @Operation(
            operationId = "getUser",
            summary = "Consultar usuario por ID",
            description = "Obtiene el detalle de un usuario por su userId. Un administrador puede consultar cualquier usuario; el resto solo la propia cuenta."
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<UserResponse>> get(
            @PathVariable String userId,
            HttpServletRequest httpRequest
    ) {
        User user = getUserByIdUseCase.execute(userId);
        return responseFactory.success(httpRequest, HttpStatus.OK, UserResponse.from(user));
    }
}
