package com.indicore.infrastructure.in.rest.roles;

import com.indicore.application.user.usecase.ListRolesUseCase;
import com.indicore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.indicore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.indicore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.indicore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(
        name = "Roles",
        description = "Catálogo de roles para el dropdown Rol del formulario de usuarios (p. ej. Administrador, Operador)."
)
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final ListRolesUseCase listRolesUseCase;
    private final ApiResponseFactory responseFactory;

    public RoleController(ListRolesUseCase listRolesUseCase, ApiResponseFactory responseFactory) {
        this.listRolesUseCase = listRolesUseCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "listRoles",
            summary = "Listar roles",
            description = """
                    Devuelve el catálogo de roles (`code` + `name`).
                    Usar el campo `code` como `roleCode` al registrar o actualizar usuarios.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Catálogo de roles",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<RoleResponse>>> list(HttpServletRequest httpRequest) {
        List<RoleResponse> data = listRolesUseCase.execute().stream()
                .map(role -> new RoleResponse(role.getRoleId(), role.getCode(), role.getName()))
                .toList();
        return responseFactory.success(httpRequest, HttpStatus.OK, data);
    }
}
