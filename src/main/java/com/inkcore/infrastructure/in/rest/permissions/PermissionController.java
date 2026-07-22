package com.inkcore.infrastructure.in.rest.permissions;

import com.inkcore.application.user.usecase.ListPermissionsUseCase;
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
@RequestMapping("/api/v1/permissions")
@Tag(
        name = "Permisos",
        description = "Catálogo de permisos para los checkboxes del formulario de alta/edición de usuarios."
)
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

    private final ListPermissionsUseCase listPermissionsUseCase;
    private final ApiResponseFactory responseFactory;

    public PermissionController(
            ListPermissionsUseCase listPermissionsUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.listPermissionsUseCase = listPermissionsUseCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "listPermissions",
            summary = "Listar permisos",
            description = """
                    Devuelve el catálogo de permisos (`code` + `name`).
                    Usar los valores de `code` en el arreglo `permissionCodes` al registrar o actualizar usuarios.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Catálogo de permisos",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<PermissionResponse>>> list(HttpServletRequest httpRequest) {
        List<PermissionResponse> data = listPermissionsUseCase.execute().stream()
                .map(p -> new PermissionResponse(p.getPermissionId(), p.getCode(), p.getName()))
                .toList();
        return responseFactory.success(httpRequest, HttpStatus.OK, data);
    }
}
