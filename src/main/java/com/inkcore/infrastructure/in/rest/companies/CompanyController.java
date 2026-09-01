package com.inkcore.infrastructure.in.rest.companies;

import com.inkcore.application.company.usecase.ListCompanyIdsNamesUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.CompanyIdNameListSuccessEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@RequestMapping("/api/v1/companies")
@Tag(name = "Empresas", description = "Catálogo de empresas")
@SecurityRequirement(name = "bearerAuth")
public class CompanyController {

    private final ListCompanyIdsNamesUseCase listCompanyIdsNamesUseCase;
    private final ApiResponseFactory responseFactory;

    public CompanyController(
            ListCompanyIdsNamesUseCase listCompanyIdsNamesUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.listCompanyIdsNamesUseCase = listCompanyIdsNamesUseCase;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/ids-names")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            operationId = "listCompanyIdsNames",
            summary = "Listar empresas (solo id y nombre)",
            description = "Devuelve todas las empresas registradas en indicolors.companies como {id, name}."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Catálogo de empresas",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CompanyIdNameListSuccessEnvelope.class),
                    examples = @ExampleObject(
                            name = "EmpresasIdNombre",
                            value = """
                                    {
                                      "headers": {
                                        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                        "statusCode": 200,
                                        "code": "OK",
                                        "description": "Success"
                                      },
                                      "timestamp": "2026-08-27T18:00:00Z",
                                      "data": [
                                        {
                                          "id": "company-seed-001",
                                          "name": "InkCore S.A.S."
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<List<CompanyIdNameResponse>>> listIdsNames(
            HttpServletRequest httpRequest
    ) {
        List<CompanyIdNameResponse> data = listCompanyIdsNamesUseCase.execute().stream()
                .map(item -> new CompanyIdNameResponse(item.companyId(), item.name()))
                .toList();
        return responseFactory.success(httpRequest, HttpStatus.OK, data);
    }
}
