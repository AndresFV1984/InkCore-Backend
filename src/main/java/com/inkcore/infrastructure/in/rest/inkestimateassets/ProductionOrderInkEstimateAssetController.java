package com.inkcore.infrastructure.in.rest.inkestimateassets;

import com.inkcore.application.inkestimateasset.usecase.GetInkEstimateAssetSignedUrlUseCase;
import com.inkcore.application.inkestimateasset.usecase.InkEstimateAssetPresignResult;
import com.inkcore.application.inkestimateasset.usecase.PresignProductionOrderInkEstimateAssetUseCase;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.InkEstimateAssetSignedUrlSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.InkEstimateAssetUploadSuccessEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/production-orders/{productionOrderId}/ink-estimate-assets")
@Tag(name = "Archivos estimación tinta", description = "Presign PUT y URLs firmadas de artes de OP")
@SecurityRequirement(name = "bearerAuth")
public class ProductionOrderInkEstimateAssetController {

    private final PresignProductionOrderInkEstimateAssetUseCase presignUseCase;
    private final GetInkEstimateAssetSignedUrlUseCase signedUrlUseCase;
    private final ApiResponseFactory responseFactory;

    public ProductionOrderInkEstimateAssetController(
            PresignProductionOrderInkEstimateAssetUseCase presignUseCase,
            GetInkEstimateAssetSignedUrlUseCase signedUrlUseCase,
            ApiResponseFactory responseFactory
    ) {
        this.presignUseCase = presignUseCase;
        this.signedUrlUseCase = signedUrlUseCase;
        this.responseFactory = responseFactory;
    }

    @PostMapping(value = "/uploads", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "presignProductionOrderInkEstimateAsset",
            summary = "URL prefirmada de carga en OP existente.",
            description = "JSON (nunca multipart). Keys bajo company/{companyId}/production-orders/{id}/prints/... "
                    + "404 si la OP o plancha no pertenecen a la empresa del JWT."
    )
    @ApiResponse(
            responseCode = "201",
            description = "URLs de carga prefirmadas",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InkEstimateAssetUploadSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<InkEstimateAssetUploadResponse>> presign(
            @Parameter(description = "ID de la OP", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable String productionOrderId,
            @RequestBody InkEstimateAssetPresignRequest body,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        InkEstimateAssetPresignResult result = presignUseCase.execute(
                productionOrderId,
                InkEstimateAssetController.toCommand(body),
                authentication
        );
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Ink estimate asset upload reserved",
                InkEstimateAssetController.toResponse(result)
        );
    }

    @GetMapping("/url")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getProductionOrderInkEstimateAssetSignedUrl",
            summary = "URL firmada de lectura para objeto de una OP.",
            description = "Valida que objectKey pertenezca a la OP y a la empresa del JWT."
    )
    @ApiResponse(
            responseCode = "200",
            description = "URL firmada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InkEstimateAssetSignedUrlSuccessEnvelope.class)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<InkEstimateAssetSignedUrlResponse>> signedUrl(
            @PathVariable String productionOrderId,
            @RequestParam String objectKey,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var result = signedUrlUseCase.execute(objectKey, productionOrderId, authentication);
        return responseFactory.okStandard(httpRequest, new InkEstimateAssetSignedUrlResponse(
                result.url(), result.expiresInSeconds()));
    }
}
