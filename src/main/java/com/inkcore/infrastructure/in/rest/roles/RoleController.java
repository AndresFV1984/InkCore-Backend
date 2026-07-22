package com.inkcore.infrastructure.in.rest.roles;

import com.inkcore.application.user.usecase.ListRolesUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
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
        description = "Catálogo de roles. Usar `code` o `name` en el campo `role` al crear/registrar usuarios."
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
                    Usar `code` (p. ej. ADMINISTRADOR) o `name` (p. ej. Administrador)
                    en el campo `role` de POST /api/v1/users o /api/v1/users/register.
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
                .map(role -> new RoleResponse(
                        role.getRoleId(),
                        role.getCompanyId(),
                        role.getCode(),
                        role.getName(),
                        role.getDescription(),
                        role.isState(),
                        role.getPermissionCodes()
                ))
                .toList();
        return responseFactory.success(httpRequest, HttpStatus.OK, data);
    }
}
