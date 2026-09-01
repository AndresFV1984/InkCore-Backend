package com.inkcore.infrastructure.in.rest.inkestimateassets;

import com.inkcore.application.inkestimateasset.usecase.GetInkEstimateAssetSignedUrlUseCase;
import com.inkcore.application.inkestimateasset.usecase.InkEstimateAssetPresignResult;
import com.inkcore.application.inkestimateasset.usecase.PresignInkEstimateAssetCommand;
import com.inkcore.application.inkestimateasset.usecase.PresignStagingInkEstimateAssetUseCase;
import com.inkcore.domain.objectstorage.model.PresignedUpload;
import com.inkcore.infrastructure.in.rest.envelope.ApiResponseFactory;
import com.inkcore.infrastructure.in.rest.envelope.ApiSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.ApiErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.ApiSecuredErrorResponses;
import com.inkcore.infrastructure.in.rest.openapi.InkEstimateAssetSignedUrlSuccessEnvelope;
import com.inkcore.infrastructure.in.rest.openapi.InkEstimateAssetUploadSuccessEnvelope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ink-estimate-assets")
@Tag(name = "Archivos estimación tinta", description = "Presign PUT y URLs firmadas de artes de OP")
@SecurityRequirement(name = "bearerAuth")
public class InkEstimateAssetController {

    private final PresignStagingInkEstimateAssetUseCase presignUseCase;
    private final GetInkEstimateAssetSignedUrlUseCase signedUrlUseCase;
    private final ApiResponseFactory responseFactory;

    public InkEstimateAssetController(
            PresignStagingInkEstimateAssetUseCase presignUseCase,
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
            operationId = "presignStagingInkEstimateAsset",
            summary = "URL prefirmada de carga (OP nueva, staging tmp/).",
            description = "JSON con entradaId (obligatorio), fileName/contentType/sizeBytes. "
                    + "plateId es opcional en staging (antes de crear la OP). "
                    + "El navegador hace PUT directo a S3. "
                    + "No acepta multipart ni bytes. Máx. según inkcore.object-storage.max-asset-file-bytes."
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
    public ResponseEntity<ApiSuccessEnvelope<InkEstimateAssetUploadResponse>> presignStaging(
            @RequestBody InkEstimateAssetPresignRequest body,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        InkEstimateAssetPresignResult result = presignUseCase.execute(toCommand(body), authentication);
        return responseFactory.created(
                httpRequest,
                "CREATED",
                "Ink estimate asset upload reserved",
                toResponse(result)
        );
    }

    @GetMapping("/url")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('OPERADOR')")
    @Operation(
            operationId = "getStagingInkEstimateAssetSignedUrl",
            summary = "URL firmada de lectura (staging u objeto de la empresa).",
            description = "Bucket privado. TTL configurable (default 300 s). Valida companyId del JWT."
    )
    @ApiResponse(
            responseCode = "200",
            description = "URL firmada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InkEstimateAssetSignedUrlSuccessEnvelope.class),
                    examples = @ExampleObject(value = """
                            {
                              "headers": {"statusCode": 200, "code": "OK", "description": "Success"},
                              "timestamp": "2026-08-15T17:00:00Z",
                              "data": {"url": "https://minio.example/presigned", "expiresInSeconds": 300}
                            }
                            """)
            )
    )
    @ApiErrorResponses
    @ApiSecuredErrorResponses
    public ResponseEntity<ApiSuccessEnvelope<InkEstimateAssetSignedUrlResponse>> signedUrl(
            @Parameter(description = "Clave devuelta por upload", required = true)
            @RequestParam String objectKey,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        var result = signedUrlUseCase.execute(objectKey, null, authentication);
        return responseFactory.okStandard(httpRequest, new InkEstimateAssetSignedUrlResponse(
                result.url(), result.expiresInSeconds()));
    }

    static PresignInkEstimateAssetCommand toCommand(InkEstimateAssetPresignRequest body) {
        InkEstimateAssetPresignRequest request = body == null
                ? new InkEstimateAssetPresignRequest(null, null, null, null, null, null, null, null, null)
                : body;
        return new PresignInkEstimateAssetCommand(
                request.plateId(),
                request.entradaId(),
                request.fileName(),
                request.contentType(),
                request.sizeBytes(),
                request.previewFileName(),
                request.previewContentType(),
                request.previewSizeBytes(),
                request.existingObjectKey()
        );
    }

    static InkEstimateAssetUploadResponse toResponse(InkEstimateAssetPresignResult result) {
        return new InkEstimateAssetUploadResponse(
                result.objectKey(),
                result.previewObjectKey(),
                result.contentType(),
                result.sizeBytes(),
                result.fileName(),
                toPart(result.upload()),
                toPart(result.previewUpload())
        );
    }

    private static InkEstimateAssetUploadResponse.InkEstimatePresignedPartResponse toPart(PresignedUpload upload) {
        if (upload == null) {
            return null;
        }
        return new InkEstimateAssetUploadResponse.InkEstimatePresignedPartResponse(
                upload.url(),
                upload.method(),
                upload.headers(),
                upload.expiresIn().toSeconds()
        );
    }
}
