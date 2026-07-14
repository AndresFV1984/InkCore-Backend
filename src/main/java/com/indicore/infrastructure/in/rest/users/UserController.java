package com.indicore.infrastructure.in.rest.users;

import com.indicore.application.user.usecase.CreateUserCommand;
import com.indicore.application.user.usecase.CreateUserUseCase;
import com.indicore.application.user.usecase.GetUserByIdUseCase;
import com.indicore.application.user.usecase.ListUsersUseCase;
import com.indicore.application.user.usecase.UpdateUserCommand;
import com.indicore.application.user.usecase.UpdateUserUseCase;
import com.indicore.domain.user.model.User;
import com.indicore.infrastructure.in.rest.envelope.ApiErrorEnvelope;
import com.indicore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.indicore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.indicore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.indicore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(
        name = "Usuarios",
        description = """
                Gestión de usuarios del sistema alineada al formulario Nuevo usuario:
                datos personales, rol, permisos, contraseña y estado.
                Requiere JWT Bearer. Las operaciones de alta, actualización y listado
                exigen rol ADMINISTRADOR.
                """
)
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final ApiResponseFactory responseFactory;

    public UserController(
            CreateUserUseCase createUserUseCase,
            UpdateUserUseCase updateUserUseCase,
            ListUsersUseCase listUsersUseCase,
            GetUserByIdUseCase getUserByIdUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "registerUser",
            summary = "Registrar usuario",
            description = """
                    Crea un usuario con los campos del formulario (nombre, documento, correo,
                    departamento, ciudad, rol, permisos, contraseña y estado).
                    La contraseña se almacena con hash (mínimo 6 caracteres).
                    Obtener `roleCode` y `permissionCodes` desde `/api/v1/roles/list` y `/api/v1/permissions/list`.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Usuario registrado correctamente",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class)
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
                            schema = @Schema(implementation = CreateUserRequest.class),
                            examples = @ExampleObject(
                                    name = "OperadorActivo",
                                    summary = "Alta de operador activo",
                                    value = """
                                            {
                                              "name": "María Fernanda López",
                                              "documentType": "CC",
                                              "identificationNumber": "1234567890",
                                              "mail": "maria.lopez@empresa.com",
                                              "contact": "300 123 4567",
                                              "department": "Antioquia",
                                              "city": "Medellín",
                                              "address": "Calle 10 # 20-30, El Poblado",
                                              "roleCode": "OPERADOR",
                                              "permissionCodes": [
                                                "CREAR_EDITAR_ORDENES_PRODUCCION",
                                                "VER_ORDENES_PRODUCCION",
                                                "GESTIONAR_MI_TRABAJO_PRODUCCION"
                                              ],
                                              "password": "clave123",
                                              "state": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest
    ) {
        User created = createUserUseCase.execute(toCreateCommand(request));
        return responseFactory.success(httpRequest, HttpStatus.CREATED, UserResponse.from(created));
    }

    @PutMapping("/update/{userId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "updateUser",
            summary = "Actualizar usuario",
            description = """
                    Actualiza datos personales, rol, permisos y estado.
                    La contraseña es opcional: si no se envía (o va vacía), se conserva la actual;
                    si se envía, debe tener mínimo 6 caracteres.
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
                                              "roleCode": "OPERADOR",
                                              "permissionCodes": [
                                                "VER_ORDENES_PRODUCCION",
                                                "MARCAR_IMPRESION_EN_PROCESO"
                                              ],
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
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "listUsers",
            summary = "Listar usuarios",
            description = "Devuelve todos los usuarios con rol, permisos y estado para administración."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Listado de usuarios",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class)
            )
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

    private static CreateUserCommand toCreateCommand(CreateUserRequest request) {
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
                request.roleCode(),
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
                request.roleCode(),
                request.permissionCodes() != null ? request.permissionCodes() : List.of(),
                Boolean.TRUE.equals(request.state())
        );
    }
}
