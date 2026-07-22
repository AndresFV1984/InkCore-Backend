package com.inkcore.infrastructure.in.rest.users;

import com.inkcore.application.user.usecase.CreateUserCommand;
import com.inkcore.application.user.usecase.CreateUserUseCase;
import com.inkcore.application.user.usecase.GetUserByIdUseCase;
import com.inkcore.application.user.usecase.ListUsersUseCase;
import com.inkcore.application.user.usecase.LoginResult;
import com.inkcore.application.user.usecase.LoginUserCommand;
import com.inkcore.application.user.usecase.LoginUserUseCase;
import com.inkcore.application.user.usecase.UpdateUserCommand;
import com.inkcore.application.user.usecase.UpdateUserUseCase;
import com.inkcore.domain.user.model.User;
import com.inkcore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.UserListSuccessEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/users")
@Tag(
        name = "Usuarios",
        description = """
                Login público; alta (`POST /users` o `/users/register` con campo `role`),
                listado y actualización requieren JWT Bearer.
                Respuesta: documentType/department anidados y roles[{role, permissions}].
                """
)
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final LoginUserUseCase loginUserUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public UserController(
            LoginUserUseCase loginUserUseCase,
            CreateUserUseCase createUserUseCase,
            UpdateUserUseCase updateUserUseCase,
            ListUsersUseCase listUsersUseCase,
            GetUserByIdUseCase getUserByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.loginUserUseCase = loginUserUseCase;
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/login")
    @Operation(
            operationId = "login",
            summary = "Iniciar sesión",
            description = """
                    Autentica con correo y contraseña (público, sin Bearer).
                    Emite access JWT + refresh opaco en `headers` del envelope
                    (`token`, `tokenAccesExpira`, `refreshToken`, `tokenRefresExpira`).
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
                                        "status": "200",
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
    @SecurityRequirements // público: no exige Bearer (anula el @SecurityRequirement de la clase)
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

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_USUARIO_CREAR') or hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "createUser",
            summary = "Crear usuario",
            description = """
                    Crea un usuario. No emite tokens (usar login).
                    Requiere `PERMISSION_USUARIO_CREAR` o rol `ADMINISTRADOR`.
                    Enviar `role` (código o nombre, p. ej. `ADMINISTRADOR`); crea `user_roles`
                    y toma los permisos del rol en BD. Alias: `roleName`.
                    Si el rol no existe → 404 ROLE_NOT_FOUND.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Usuario creado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "UsuarioCreado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "User created"
                                      },
                                      "data": {
                                        "userId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "documentType": {
                                          "documentType": "CC",
                                          "identificationNumber": "1234567890"
                                        },
                                        "name": "Ana García",
                                        "mail": "ana@itm.edu.co",
                                        "contact": "3001234567",
                                        "department": {
                                          "department": "Antioquia",
                                          "city": "Medellín"
                                        },
                                        "address": "Calle 10 # 20-30",
                                        "creationDate": "2026-07-18",
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
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<UserResponse>> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateUserRequest.class),
                            examples = @ExampleObject(
                                    name = "NuevoUsuario",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "identificationNumber": "1234567890",
                                              "documentType": "CC",
                                              "name": "Ana García",
                                              "mail": "ana@itm.edu.co",
                                              "contact": "3001234567",
                                              "department": "Antioquia",
                                              "city": "Medellín",
                                              "address": "Calle 10 # 20-30",
                                              "password": "SecurePass123*",
                                              "role": "ADMINISTRADOR",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest
    ) {
        User created = createUserUseCase.create(
                request.companyId(),
                request.identificationNumber(),
                request.documentType(),
                request.name(),
                request.mail(),
                request.contact(),
                request.department(),
                request.city(),
                request.address(),
                request.password(),
                request.resolvedRole(),
                request.state()
        );
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "User created",
                UserResponse.from(created)
        );
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "registerUser",
            summary = "Registrar usuario (formulario completo)",
            description = """
                    Crea un usuario. El campo `role` es obligatorio (también acepta `roleCode` / `roleName`).
                    Crea la relación en `user_roles` y toma los permisos del rol en BD.
                    Preferir `POST /api/v1/users` para el contrato simplificado.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Usuario registrado correctamente",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "UsuarioRegistrado",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 201,
                                        "code": "CREATED",
                                        "description": "User created"
                                      },
                                      "data": {
                                        "userId": "714ad646-c4fe-42fa-9f13-4a44823e6bee",
                                        "companyId": "company-seed-001",
                                        "documentType": {
                                          "documentType": "CC",
                                          "identificationNumber": "123456789"
                                        },
                                        "name": "Bayron Morale",
                                        "mail": "bayron@indicolors.com",
                                        "contact": "111111111",
                                        "department": {
                                          "department": "Antioquia",
                                          "city": "Medellín"
                                        },
                                        "address": "Calle 10 # 20-30",
                                        "creationDate": "2026-07-18",
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
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<UserResponse>> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Payload del formulario Nuevo usuario",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegisterUserRequest.class),
                            examples = @ExampleObject(
                                    name = "OperadorActivo",
                                    summary = "Alta de operador activo",
                                    value = """
                                            {
                                              "companyId": "company-seed-001",
                                              "name": "Bayron Morale",
                                              "documentType": "CC",
                                              "identificationNumber": "123456789",
                                              "mail": "bayron@indicolors.com",
                                              "contact": "111111111",
                                              "department": "Antioquia",
                                              "city": "Medellín",
                                              "address": "Calle 10 # 20-30",
                                              "role": "ADMINISTRADOR",
                                              "password": "SecurePass123*",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody RegisterUserRequest request,
            HttpServletRequest httpRequest
    ) {
        User created = createUserUseCase.execute(toCreateCommand(request));
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "User created",
                UserResponse.from(created)
        );
    }

    @PutMapping("/update/{userId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "updateUser",
            summary = "Actualizar usuario",
            description = """
                    Actualiza datos personales, rol y estado.
                    Enviar `role` (código o nombre); alias `roleCode`.
                    La contraseña es opcional: si no se envía (o va vacía), se conserva la actual;
                    si se envía, debe tener mínimo 6 caracteres.
                    Respuesta con documentType/department anidados y roles[{role, permissions}].
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Usuario actualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<UserResponse>> update(
            @Parameter(
                    description = "Identificador del usuario a actualizar",
                    required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            )
            @PathVariable String userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Datos a actualizar",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateUserRequest.class),
                            examples = @ExampleObject(
                                    name = "ActualizarSinPassword",
                                    summary = "Actualización sin cambio de contraseña",
                                    value = """
                                            {
                                              "name": "María Fernanda López",
                                              "documentType": "CC",
                                              "identificationNumber": "1234567890",
                                              "mail": "maria.lopez@empresa.com",
                                              "contact": "300 987 6543",
                                              "department": "Antioquia",
                                              "city": "Envigado",
                                              "address": "Carrera 40 # 15-20",
                                              "role": "OPERADOR",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest
    ) {
        User updated = updateUserUseCase.execute(toUpdateCommand(userId, request));
        return responseFactory.success(httpRequest, HttpStatus.OK, UserResponse.from(updated));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('PERMISSION_USUARIO_VER') or hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "listUsers",
            summary = "Listar usuarios",
            description = """
                    Devuelve usuarios con datos para el Directorio y el formulario de edición
                    (incluye `address`, documentType/department anidados y roles[{role, permissions}]).
                    Query opcional `state`: true=activos, false=inactivos, ausente=todos.
                    Requiere JWT con `PERMISSION_USUARIO_VER` o rol `ADMINISTRADOR`.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de usuarios",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "Usuarios",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-07-18T06:00:00Z",
                                      "data": [
                                        {
                                          "userId": "seed-cfg-9f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c",
                                          "companyId": "company-seed-001",
                                          "documentType": {
                                            "documentType": "NIT",
                                            "identificationNumber": "9001234567"
                                          },
                                          "name": "Administrador InkCore",
                                          "mail": "admin@indicolors.com",
                                          "contact": "3001234567",
                                          "department": {
                                            "department": "Antioquia",
                                            "city": "Medellín"
                                          },
                                          "address": "Medellín, Colombia",
                                          "creationDate": "2026-07-18",
                                          "state": true,
                                          "forcePasswordChange": false,
                                          "failedAttempts": 0,
                                          "roles": [
                                            {
                                              "role": "ADMINISTRADOR",
                                              "permissions": ["PERMISSION_USUARIO_VER", "PERMISSION_USUARIO_CREAR"]
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<UserListItemResponse>>> listUsers(
            @Parameter(description = "Filtro por estado: true=activos, false=inactivos, omitir=todos")
            @RequestParam(required = false) Boolean state,
            HttpServletRequest httpRequest
    ) {
        List<UserListItemResponse> data = listUsersUseCase.execute(state).stream()
                .map(UserListItemResponse::from)
                .toList();
        return responseFactory.okStandard(httpRequest, data);
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "profileUser",
            summary = "Mi perfil",
            description = "Devuelve el usuario autenticado según el claim `sub` del JWT."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Perfil del token actual",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<UserResponse>> profile(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        User user = getUserByIdUseCase.execute(authentication.getName());
        return responseFactory.success(httpRequest, HttpStatus.OK, UserResponse.from(user));
    }

    @GetMapping("/get/{userId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or authentication.name == #userId")
    @Operation(
            operationId = "getUser",
            summary = "Consultar usuario por ID",
            description = "Detalle de un usuario. ADMINISTRADOR puede consultar cualquiera; el resto solo su propia cuenta."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Usuario encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<UserResponse>> get(
            @Parameter(
                    description = "Identificador del usuario",
                    required = true,
                    example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            )
            @PathVariable String userId,
            HttpServletRequest httpRequest
    ) {
        User user = getUserByIdUseCase.execute(userId);
        return responseFactory.success(httpRequest, HttpStatus.OK, UserResponse.from(user));
    }

    private static CreateUserCommand toCreateCommand(RegisterUserRequest request) {
        return new CreateUserCommand(
                request.companyId(),
                request.identificationNumber(),
                request.documentType(),
                request.name(),
                request.mail(),
                request.contact(),
                request.department(),
                request.city(),
                request.address(),
                request.password(),
                request.resolvedRole(),
                request.permissionCodes() != null ? request.permissionCodes() : List.of(),
                Boolean.TRUE.equals(request.state())
        );
    }

    private static UpdateUserCommand toUpdateCommand(String userId, UpdateUserRequest request) {
        return new UpdateUserCommand(
                userId,
                request.identificationNumber(),
                request.documentType(),
                request.name(),
                request.mail(),
                request.contact(),
                request.department(),
                request.city(),
                request.address(),
                request.password(),
                request.resolvedRole(),
                request.permissionCodes() != null ? request.permissionCodes() : List.of(),
                Boolean.TRUE.equals(request.state())
        );
    }
}
